package com.hrms.service;

import com.hrms.common.SecurityUtil;
import com.hrms.domain.attendance.AttendanceRecord;
import com.hrms.domain.employee.Employee;
import com.hrms.domain.payroll.PayrollRecord;
import com.hrms.domain.performance.PerformanceRecord;
import com.hrms.domain.training.TrainingRecord;
import com.hrms.dto.AssistantReply;
import com.hrms.repository.*;
import com.hrms.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 智能助手：规则式自然语言意图引擎（无需外部 LLM）。
 * 通过关键词/同义词匹配用户提问 → 复用 AnalyticsService / DashboardService 数据能力 → 组装
 * 「自然语言回答 + 图表 + 表格 + 推荐追问」。支持轻量多轮上下文（追问续接）与角色敏感数据收敛。
 */
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final AnalyticsService analyticsService;
    private final DashboardService dashboardService;
    private final EmployeeRepository employeeRepository;
    private final PerformanceRecordRepository performanceRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final PayrollRecordRepository payrollRepository;
    private final TrainingRecordRepository trainingRecordRepository;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int HISTORY_LIMIT = 50;

    /** 会话历史：username → 最近对话（用于页面刷新后恢复） */
    private final Map<String, ArrayDeque<Map<String, Object>>> histories = new ConcurrentHashMap<>();
    /** 用户名 → 最近访问时间（用于历史记录上限淘汰） */
    private final Map<String, Long> historyLastAccess = new ConcurrentHashMap<>();
    /** 历史记录允许的最大用户名数（防止无界增长） */
    private static final int HISTORY_USER_LIMIT = 200;
    /** 最近一次命中的意图（用于追问续接） */
    private final Map<String, String> lastIntent = new ConcurrentHashMap<>();
    /** 追问信号词：仅当未识别问题含这些词时，才把它当作上一话题的续接（避免乱码/新话题被误重放） */
    private static final List<String> FOLLOWUP_HINTS = List.of(
            "那", "呢", "这个", "这些", "哪个", "继续", "接着", "然后", "再", "更", "它", "他", "她们", "他们");

    private static final List<String> DEPT_NAMES = List.of(
            "集团总部", "技术中心", "后端研发部", "前端研发部", "测试部", "运维部", "数据智能部",
            "产品中心", "产品部", "设计部", "运营中心", "运营部", "市场部", "客服部",
            "销售中心", "大客户销售部", "渠道销售部", "职能中心", "人力资源部", "财务部", "行政部", "法务部");

    // ==================== 入口 ====================

    public AssistantReply ask(String question) {
        LoginUser user = SecurityUtil.currentUser();
        String q = normalize(question);
        String username = user.username();
        recordChat(username, "user", q);

        AssistantReply reply = dispatch(q, user);
        lastIntent.put(username, reply.intent());
        recordAssistantChat(username, reply);
        return reply;
    }

    private AssistantReply dispatch(String q, LoginUser user) {
        // 帮助
        if (containsAny(q, "帮助", "能做什么", "你会什么", "有什么功能", "怎么用", "支持哪些", "指南", "help")) return help();
        // 个人相关（先于通用薪资/绩效，保证「我的工资」命中个人）
        if (containsAny(q, "我的", "我这个", "我最近", "我自己")) return personal(q, user);
        // 员工角色收敛：管理端分析数据（招聘/绩效/人效/人才盘点/结构/趋势/全司考勤/培训统计等）
        // 仅对 HR / 管理者开放，员工提问时降级为友好提示（公司规模、组织架构、个人自助不受限）
        if ("EMPLOYEE".equals(user.role()) && containsAny(q,
                "人效", "人均", "招聘", "漏斗", "候选人", "渠道", "BOSS", "内推", "拉勾", "猎聘", "校园", "校招",
                "九宫格", "盘点", "潜力", "高潜", "绩效", "评级", "考核", "评分",
                "结构", "分布", "构成", "年龄", "学历", "职级", "性别", "司龄",
                "趋势", "增长", "变动", "走势",
                "成本", "工资总额", "薪酬总额", "人力成本", "平均薪资", "平均工资",
                "出勤率", "迟到", "缺勤", "全勤", "培训统计", "学习统计", "能力", "雷达", "画像")) {
            return managementDenied();
        }
        // 组织架构
        if (containsAny(q, "组织架构", "组织", "架构", "部门树", "层级")) return orgOverview(q, user);
        // 部门人效（先于部门人数，避免「部门人效」被吞）
        if (containsAny(q, "人效", "人均", "人均效率")) return deptEfficiency(q, user);
        // 部门人数（含指定部门名 / 最多最少）
        if (extractDept(q) != null || containsAny(q, "部门", "人数最多", "人数最少", "哪个部门")) return deptHeadcount(q, user);
        // 离职
        if (containsAny(q, "离职", "流失", "走了", "跳槽")) return resignations(q, user);
        // 入职
        if (containsAny(q, "入职", "新进", "新招", "招人")) return newHires(user);
        // 总人数
        if (containsAny(q, "多少人", "员工数", "总人数", "在职", "有几个", "公司规模", "总共有")) return overview(q, user);
        // 薪酬成本（含平均薪资）
        if (containsAny(q, "成本", "工资总额", "薪酬总额", "人力成本")) return costTrend(user);
        if (containsAny(q, "平均薪资", "平均工资", "薪资", "工资", "月薪", "多少钱")) return avgSalary(user);
        // 出勤考勤
        if (containsAny(q, "出勤", "考勤", "迟到", "缺勤", "全勤")) return attendance(q, user);
        // 招聘（漏斗 / 渠道 / 候选人）
        if (containsAny(q, "招聘", "漏斗", "候选人", "渠道", "BOSS", "内推", "拉勾", "猎聘", "官网", "校园", "校招")) return recruitment(q, user);
        // 人才盘点九宫格
        if (containsAny(q, "九宫格", "人才盘点", "盘点", "潜力", "高潜")) return nineBox(user);
        // 绩效（先于结构，避免「绩效分布」被「分布」吞掉）
        if (containsAny(q, "绩效", "评级", "考核", "评分")) return performance(user);
        // 人力结构
        if (containsAny(q, "结构", "分布", "构成", "年龄", "学历", "职级", "性别", "司龄")) return structure(q, user);
        // 趋势
        if (containsAny(q, "趋势", "增长", "变动", "走势", "发展")) return trend(user);
        // 培训
        if (containsAny(q, "培训", "课程", "学习", "能力提升")) return training(user);
        // 能力雷达
        if (containsAny(q, "能力", "雷达", "综合画像")) return radar(user);
        // 兜底：尝试追问续接
        return fallback(q, user);
    }

    // ==================== 帮助 ====================

    private AssistantReply help() {
        String text = """
                你好，我是 HR 智能助手 👋 我能基于系统数据回答人力资源问题，例如：
                · 公司规模：　「现在在职多少人？」「本月入职/离职多少？」
                · 部门分析：　「各部门人数」「哪个部门人效最高」
                · 薪酬成本：　「平均薪资多少」「近半年人力成本趋势」
                · 招聘动态：　「招聘漏斗」「各渠道效能如何」
                · 团队画像：　「年龄/学历结构」「人才盘点九宫格」「出勤率」
                · 我的数据：　「我的考勤」「我的绩效」「我的工资」
                直接输入问题即可，也可以点下方的推荐问题。
                """.stripIndent();
        return AssistantReply.text("HELP", text, baseSuggestions());
    }

    private AssistantReply fallback(String q, LoginUser user) {
        String last = lastIntent.get(user.username());
        // 仅当问题带追问信号词时才续接上一话题；否则就是新问题/乱码，走真正兜底
        boolean followUp = containsAny(q, FOLLOWUP_HINTS.toArray(new String[0]));
        if (last != null && followUp && !"HELP".equals(last) && !"FALLBACK".equals(last) && !"RBAC_DENIED".equals(last)) {
            // 追问续接：复用上一个话题的处理逻辑，返回续接后的真实意图
            AssistantReply r = replay(last, q, user);
            if (!"HELP".equals(r.intent())) {
                String note = "（接着刚才的话题）";
                // 保留原回复携带的图表/表格，避免续接后可视化丢失
                return new AssistantReply(r.intent(), r.title(), note + r.reply(), r.chart(), r.table(), r.suggestions());
            }
        }
        return AssistantReply.text("FALLBACK",
                "抱歉，这个问题我还没学会 😅 你可以换一种说法，比如「在职多少人」「各部门人数」「平均薪资」「招聘漏斗」「我的考勤」，或点下面的推荐问题。",
                baseSuggestions());
    }

    private AssistantReply replay(String intent, String q, LoginUser user) {
        return switch (intent) {
            case "DEPT_HEADCOUNT" -> deptHeadcount(q, user);
            case "RESIGNATIONS" -> resignations(q, user);
            case "NEW_HIRES" -> newHires(user);
            case "OVERVIEW" -> overview(q, user);
            case "AVG_SALARY" -> avgSalary(user);
            case "ATTENDANCE" -> attendance(q, user);
            case "RECRUITMENT" -> recruitment(q, user);
            case "NINE_BOX" -> nineBox(user);
            case "STRUCTURE" -> structure(q, user);
            case "DEPT_EFFICIENCY" -> deptEfficiency(q, user);
            case "COST_TREND" -> costTrend(user);
            case "TREND" -> trend(user);
            case "TRAINING" -> training(user);
            case "PERFORMANCE" -> performance(user);
            case "RADAR" -> radar(user);
            case "ORG_OVERVIEW" -> orgOverview(q, user);
            case "PERSONAL" -> personal(q, user);
            default -> help();
        };
    }

    // ==================== 员工规模 ====================

    private AssistantReply overview(String q, LoginUser user) {
        Map<String, Object> s = dashboardService.summary();
        long total = ((Number) s.get("totalEmployees")).longValue();
        long probation = ((Number) s.get("probation")).longValue();
        long newHires = ((Number) s.get("newHires")).longValue();
        long resignations = ((Number) s.get("resignations")).longValue();
        double attrition = ((Number) s.get("attritionRate")).doubleValue();
        String month = (String) s.get("payrollMonth");
        StringBuilder reply = new StringBuilder(String.format(
                "公司当前共有 **%d** 名在职员工（其中试用期 %d 人），本月新入职 %d 人、离职 %d 人（离职率 %.1f%%）。",
                total, probation, newHires, resignations, attrition));
        // 平均月薪属敏感数据：仅 HR/管理者可见（与 avgSalary 意图的 RBAC 口径一致）
        if (canSeeFinancial(user)) {
            reply.append(String.format("平均月薪 %s 元。", fmtMoney(((Number) s.get("avgSalary")).doubleValue())));
        }
        return AssistantReply.withChart("OVERVIEW", "员工规模概览（" + month + "）", reply.toString(),
                barChart("在职人数", List.of("在职员工"), List.of(List.of("人数")), List.of(List.of((double) total))),
                List.of("各部门多少人？", "本月入职多少？", "离职率趋势怎样？"));
    }

    private AssistantReply newHires(LoginUser user) {
        Map<String, Object> s = dashboardService.summary();
        long newHires = ((Number) s.get("newHires")).longValue();
        long total = ((Number) s.get("totalEmployees")).longValue();
        String reply = String.format("本月新入职 **%d** 人，目前在职总人数 %d 人。", newHires, total);
        return AssistantReply.withChart("NEW_HIRES", "本月入职", reply,
                barChart("本月入职人数", List.of("本月"), List.of(List.of("入职")), List.of(List.of((double) newHires))),
                List.of("在职多少人？", "各部门人数？", "离职率怎样？"));
    }

    private AssistantReply resignations(String q, LoginUser user) {
        Map<String, Object> s = dashboardService.summary();
        long resignations = ((Number) s.get("resignations")).longValue();
        double attrition = ((Number) s.get("attritionRate")).doubleValue();
        Map<String, Object> trend = analyticsService.attritionTrend();
        @SuppressWarnings("unchecked")
        List<String> months = (List<String>) trend.get("months");
        @SuppressWarnings("unchecked")
        List<Number> leaves = (List<Number>) trend.get("leaves");
        String reply = String.format("本月离职 **%d** 人，离职率 **%.1f%%**。近 12 个月离职人数：%s。",
                resignations, attrition, months.isEmpty() ? "-" : months.get(months.size() - 1) + " " + leaves.get(leaves.size() - 1) + "人");
        return AssistantReply.withChart("RESIGNATIONS", "近 12 个月离职趋势", reply,
                lineChart(months, List.of("离职人数"), List.of(leaves.stream().map(Number::doubleValue).toList())),
                List.of("本月入职多少？", "在职多少人？", "各部门人数？"));
    }

    private AssistantReply attendance(String q, LoginUser user) {
        Map<String, Object> s = dashboardService.summary();
        double rate = ((Number) s.get("attendanceRate")).doubleValue();
        Map<String, Object> trend = analyticsService.attendanceTrend();
        @SuppressWarnings("unchecked")
        List<String> months = (List<String>) trend.get("months");
        @SuppressWarnings("unchecked")
        List<Number> rates = (List<Number>) trend.get("rates");
        String reply = String.format("本月整体出勤率 **%.1f%%**。近 6 个月出勤率：%s。",
                rate, months.isEmpty() ? "-" : months.get(months.size() - 1) + " " + rates.get(rates.size() - 1) + "%");
        return AssistantReply.withChart("ATTENDANCE", "近 6 个月出勤率", reply,
                lineChart(months, List.of("出勤率"), List.of(rates.stream().map(Number::doubleValue).toList())),
                List.of("各部门人数？", "平均薪资多少？", "我的考勤怎么样？"));
    }

    private AssistantReply trend(LoginUser user) {
        Map<String, Object> t = dashboardService.headcountTrend();
        @SuppressWarnings("unchecked")
        List<String> months = (List<String>) t.get("months");
        @SuppressWarnings("unchecked")
        List<Number> hc = (List<Number>) t.get("headcounts");
        @SuppressWarnings("unchecked")
        List<Number> hires = (List<Number>) t.get("hires");
        @SuppressWarnings("unchecked")
        List<Number> leaves = (List<Number>) t.get("leaves");
        String last = months.isEmpty() ? "-" : months.get(months.size() - 1);
        String reply = String.format("近 12 个月人员呈%s趋势，最近月份（%s）在职 %d 人、入职 %d 人、离职 %d 人。",
                hc.size() >= 2 && hc.get(hc.size() - 1).doubleValue() >= hc.get(0).doubleValue() ? "上升" : "波动",
                last, lastVal(hc), lastVal(hires), lastVal(leaves));
        return AssistantReply.withChart("TREND", "近 12 个月人员规模趋势", reply,
                lineChart(months, List.of("在职", "入职", "离职"),
                        List.of(hc.stream().map(Number::doubleValue).toList(),
                                hires.stream().map(Number::doubleValue).toList(),
                                leaves.stream().map(Number::doubleValue).toList())),
                List.of("现在在职多少人？", "离职率怎样？", "各部门人数？"));
    }

    // ==================== 部门 ====================

    private AssistantReply deptHeadcount(String q, LoginUser user) {
        Map<String, Object> data = analyticsService.deptHeadcount();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
        String dept = extractDept(q);

        // 指定部门
        if (dept != null && containsAny(q, "多少人", "人数", "多少", "几人", "规模")) {
            long n = items.stream().filter(i -> dept.equals(i.get("name"))).mapToLong(i -> ((Number) i.get("value")).longValue()).findFirst().orElse(0);
            String reply = String.format("「%s」目前在职 **%d** 人。", dept, n);
            return AssistantReply.text("DEPT_HEADCOUNT", reply,
                    List.of("各部门多少人？", "人数最多的部门？", "这个部门人效如何？"));
        }
        // 极值
        if (containsAny(q, "最多", "最少", "最大", "排名", "排")) {
            boolean max = containsAny(q, "最多", "最大");
            items.sort(Comparator.comparingLong(i -> ((Number) i.get("value")).longValue()));
            if (max) Collections.reverse(items);
            List<String> top = items.stream().limit(5).map(i -> "· " + i.get("name") + " " + i.get("value") + "人").collect(Collectors.toList());
            String reply = String.format("%s的部门：\n%s", max ? "人数最多" : "人数最少", String.join("\n", top));
            return AssistantReply.withChart("DEPT_HEADCOUNT", max ? "人数最多的部门" : "人数最少的部门", reply,
                    barChart("部门人数", items.stream().limit(8).map(i -> (String) i.get("name")).toList(),
                            List.of(List.of("人数")), List.of(items.stream().limit(8).map(i -> ((Number) i.get("value")).doubleValue()).toList())),
                    List.of("各部门多少人？", "平均薪资多少？", "哪个部门人效最高？"));
        }
        // 全量
        StringBuilder sb = new StringBuilder("各部门在职人数：\n");
        for (Map<String, Object> i : items) sb.append("· ").append(i.get("name")).append(" ").append(i.get("value")).append("人\n");
        return AssistantReply.withChart("DEPT_HEADCOUNT", "各部门在职人数", sb.toString().trim(),
                barChart("部门人数", items.stream().map(i -> (String) i.get("name")).toList(),
                        List.of(List.of("人数")), List.of(items.stream().map(i -> ((Number) i.get("value")).doubleValue()).toList())),
                List.of("人数最多的部门？", "哪个部门人效最高？", "平均薪资多少？"));
    }

    private AssistantReply deptEfficiency(String q, LoginUser user) {
        // 部门人效含人均薪资，属敏感数据：仅 HR/管理者可看
        if (!canSeeFinancial(user)) return financialDenied();
        Map<String, Object> data = analyticsService.deptEfficiency();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = new ArrayList<>((List<Map<String, Object>>) data.get("items"));
        boolean topSalary = containsAny(q, "薪资", "工资", "高");
        items.sort(Comparator.comparingDouble(i -> ((Number) i.get("avgSalary")).doubleValue()));
        if (topSalary) Collections.reverse(items);
        List<String> top = items.stream().limit(5).map(i -> String.format("· %s（%d 人，平均 %.1f 万）",
                i.get("name"), ((Number) i.get("headcount")).longValue(), ((Number) i.get("avgSalary")).doubleValue() / 10000)).collect(Collectors.toList());
        String reply = String.format("部门人效对比（平均薪资 %s）：\n%s", topSalary ? "从高到低" : "排名", String.join("\n", top));
        return AssistantReply.withChart("DEPT_EFFICIENCY", "部门人效对比", reply,
                barChart("平均薪资(元)", items.stream().map(i -> (String) i.get("name")).toList(),
                        List.of(List.of("平均薪资")), List.of(items.stream().map(i -> ((Number) i.get("avgSalary")).doubleValue()).toList())),
                List.of("各部门多少人？", "平均薪资多少？", "哪个部门人数最多？"));
    }

    private AssistantReply orgOverview(String q, LoginUser user) {
        Map<String, Object> data = analyticsService.deptHeadcount();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
        // 只取「中心」级组织
        List<String> centers = List.of("技术中心", "产品中心", "运营中心", "销售中心", "职能中心");
        long centerSum = items.stream().filter(i -> centers.contains(i.get("name"))).mapToLong(i -> ((Number) i.get("value")).longValue()).sum();
        long total = ((Number) dashboardService.summary().get("totalEmployees")).longValue();
        List<String> lines = centers.stream().map(c -> {
            long n = items.stream().filter(i -> c.equals(i.get("name"))).mapToLong(i -> ((Number) i.get("value")).longValue()).findFirst().orElse(0);
            return "· " + c + " " + n + "人";
        }).collect(Collectors.toList());
        String reply = String.format("公司采用「集团总部 → 五大中心 → 部门」三级组织架构，在职共 **%d** 人：\n%s",
                total, String.join("\n", lines));
        return AssistantReply.text("ORG_OVERVIEW", reply,
                List.of("各部门多少人？", "人才盘点九宫格？", "各部门人效？"));
    }

    // ==================== 薪酬 ====================

    private AssistantReply avgSalary(LoginUser user) {
        if (!canSeeFinancial(user)) return financialDenied();
        Map<String, Object> s = dashboardService.summary();
        double avg = ((Number) s.get("avgSalary")).doubleValue();
        Map<String, Object> cost = analyticsService.payrollCostTrend();
        @SuppressWarnings("unchecked")
        List<String> months = (List<String>) cost.get("months");
        @SuppressWarnings("unchecked")
        List<Number> costs = (List<Number>) cost.get("costs");
        String reply = String.format("公司平均月薪 **%s** 元。近 6 个月人力成本合计约 %s 万元，最新月份（%s）应发总额 %s 万元。",
                fmtMoney(avg), fmtWan(costs.stream().mapToDouble(Number::doubleValue).sum()), months.isEmpty() ? "-" : months.get(months.size() - 1),
                costs.isEmpty() ? "0" : fmtWan(costs.get(costs.size() - 1).doubleValue()));
        return AssistantReply.withChart("AVG_SALARY", "平均薪资与人力成本", reply,
                lineChart(months, List.of("人力成本(元)"), List.of(costs.stream().map(Number::doubleValue).toList())),
                List.of("各部门人效对比？", "近半年成本趋势？", "各部门人数？"));
    }

    private AssistantReply costTrend(LoginUser user) {
        if (!canSeeFinancial(user)) return financialDenied();
        Map<String, Object> cost = analyticsService.payrollCostTrend();
        @SuppressWarnings("unchecked")
        List<String> months = (List<String>) cost.get("months");
        @SuppressWarnings("unchecked")
        List<Number> costs = (List<Number>) cost.get("costs");
        String reply = String.format("近 6 个月人力成本总额约 **%s** 万元，最新月份（%s）应发 %s 万元。",
                fmtWan(costs.stream().mapToDouble(Number::doubleValue).sum()), months.isEmpty() ? "-" : months.get(months.size() - 1),
                costs.isEmpty() ? "0" : fmtWan(costs.get(costs.size() - 1).doubleValue()));
        return AssistantReply.withChart("COST_TREND", "近 6 个月人力成本趋势", reply,
                lineChart(months, List.of("人力成本(元)"), List.of(costs.stream().map(Number::doubleValue).toList())),
                List.of("平均薪资多少？", "各部门人效对比？", "部门人数？"));
    }

    private AssistantReply financialDenied() {
        return AssistantReply.text("RBAC_DENIED",
                "抱歉，薪酬与成本数据仅对 HR / 管理者开放。如有疑问，可以咨询人力资源部，或让我帮你看看「我的工资」「我的绩效」。",
                List.of("我的工资", "我的绩效", "我的考勤"));
    }

    private AssistantReply managementDenied() {
        return AssistantReply.text("RBAC_DENIED",
                "抱歉，你问的属于管理端数据分析（如招聘、绩效、人效、人才盘点、趋势等），仅对 HR / 管理者开放。\n" +
                        "你可以问我：「现在在职多少人」「有哪些培训课程」「我的考勤」「我的工资」「我的绩效」。",
                List.of("现在在职多少人", "我的考勤", "我的工资", "我的绩效"));
    }

    // ==================== 招聘 ====================

    private AssistantReply recruitment(String q, LoginUser user) {
        boolean channel = containsAny(q, "渠道", "BOSS", "内推", "拉勾", "猎聘", "官网", "校园", "校招", "效能");
        if (channel) {
            Map<String, Object> d = analyticsService.recruitmentChannel();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) d.get("items");
            StringBuilder sb = new StringBuilder("各招聘渠道效能：\n");
            for (Map<String, Object> i : items) sb.append(String.format("· %s：投递 %d，Offer %d，转化率 %.0f%%\n",
                    i.get("channel"), ((Number) i.get("total")).longValue(), ((Number) i.get("hired")).longValue(), ((Number) i.get("rate")).doubleValue()));
            return AssistantReply.withChart("RECRUITMENT", "招聘渠道效能", sb.toString().trim(),
                    barChart("投递/Offer", items.stream().map(i -> (String) i.get("channel")).toList(),
                            List.of(List.of("投递"), List.of("Offer")),
                            List.of(items.stream().map(i -> ((Number) i.get("total")).doubleValue()).toList(),
                                    items.stream().map(i -> ((Number) i.get("hired")).doubleValue()).toList())),
                    List.of("招聘漏斗怎样？", "共多少候选人？", "各部门多少人？"));
        }
        Map<String, Object> d = analyticsService.recruitmentFunnel();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> funnel = (List<Map<String, Object>>) d.get("funnel");
        long total = ((Number) d.get("total")).longValue();
        StringBuilder sb = new StringBuilder("招聘转化漏斗（共 " + total + " 名候选人）：\n");
        for (Map<String, Object> i : funnel) sb.append("· ").append(i.get("stage")).append(" ").append(i.get("value")).append("人\n");
        return AssistantReply.withChart("RECRUITMENT", "招聘转化漏斗", sb.toString().trim(),
                funnelChart(funnel.stream().map(i -> (String) i.get("stage")).toList(),
                        funnel.stream().map(i -> ((Number) i.get("value")).doubleValue()).toList()),
                List.of("各渠道效能如何？", "共多少候选人？", "在职多少人？"));
    }

    // ==================== 人才 / 结构 / 培训 / 绩效 / 雷达 ====================

    private AssistantReply nineBox(LoginUser user) {
        Map<String, Object> d = analyticsService.nineBox();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cells = (List<Map<String, Object>>) d.get("cells");
        long total = ((Number) d.get("total")).longValue();
        long highHigh = cells.stream().filter(c -> "高".equals(c.get("potential")) && "高".equals(c.get("performance")))
                .mapToLong(c -> ((Number) c.get("value")).longValue()).sum();
        String reply = String.format("人才盘点覆盖 **%d** 名员工。其中「高潜 × 高绩效」的核心骨干 **%d** 人，建议重点关注与保留。",
                total, highHigh);
        Map<String, Object> table = Map.of(
                "columns", List.of(Map.of("key", "perf", "label", "绩效\\潜力"), Map.of("key", "low", "label", "潜力低"),
                        Map.of("key", "mid", "label", "潜力中"), Map.of("key", "high", "label", "潜力高")),
                "rows", buildNineBoxRows(cells));
        return AssistantReply.withTable("NINE_BOX", "人才盘点九宫格", reply, table,
                List.of("年龄结构怎样？", "各部门人数？", "平均薪资多少？"));
    }

    private AssistantReply structure(String q, LoginUser user) {
        String dim = "age";
        if (containsAny(q, "学历")) dim = "education";
        else if (containsAny(q, "职级", "级别")) dim = "level";
        else if (containsAny(q, "性别", "男女")) dim = "gender";
        else if (containsAny(q, "司龄", "工龄")) dim = "tenure";
        Map<String, Object> d = analyticsService.structure(dim);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) d.get("items");
        StringBuilder sb = new StringBuilder(dimLabel(dim) + "分布：\n");
        for (Map<String, Object> i : items) sb.append("· ").append(i.get("name")).append(" ").append(i.get("value")).append("人\n");
        return AssistantReply.withChart("STRUCTURE", dimLabel(dim) + "分布", sb.toString().trim(),
                pieChart(items.stream().map(i -> (String) i.get("name")).toList(),
                        items.stream().map(i -> ((Number) i.get("value")).doubleValue()).toList()),
                List.of("年龄结构怎样？", "各部门多少人？", "人才盘点九宫格？"));
    }

    private AssistantReply training(LoginUser user) {
        Map<String, Object> d = analyticsService.trainingStats();
        long courses = ((Number) d.get("courses")).longValue();
        long completed = ((Number) d.get("completed")).longValue();
        long totalRecords = ((Number) d.get("totalRecords")).longValue();
        double coverage = ((Number) d.get("coverage")).doubleValue();
        String reply = String.format("目前课程库有 **%d** 门课程，累计学习记录 %d 条（已完成 %d 条），培训覆盖率 **%.1f%%**。",
                courses, totalRecords, completed, coverage);
        return AssistantReply.text("TRAINING", reply,
                List.of("绩效分布怎样？", "我的培训完成情况？", "各部门人数？"));
    }

    private AssistantReply performance(LoginUser user) {
        // 取最新一期绩效分布
        List<PerformanceRecord> all = performanceRepository.findAll();
        String period = all.stream().map(PerformanceRecord::getPeriod).filter(Objects::nonNull)
                .max(String::compareTo).orElse(YearMonth.now().format(MONTH_FMT));
        Map<String, Object> d = analyticsService.performanceDistribution(period);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) d.get("items");
        StringBuilder sb = new StringBuilder(period + " 绩效评级分布：\n");
        for (Map<String, Object> i : items) sb.append("· ").append(i.get("name")).append(" ").append(i.get("value")).append("人\n");
        return AssistantReply.withChart("PERFORMANCE", period + " 绩效分布", sb.toString().trim(),
                pieChart(items.stream().map(i -> (String) i.get("name")).toList(),
                        items.stream().map(i -> ((Number) i.get("value")).doubleValue()).toList()),
                List.of("人才盘点九宫格？", "培训情况？", "各部门人数？"));
    }

    private AssistantReply radar(LoginUser user) {
        Map<String, Object> d = analyticsService.capabilityRadar("全公司");
        @SuppressWarnings("unchecked")
        List<String> indicators = (List<String>) d.get("indicators");
        @SuppressWarnings("unchecked")
        List<Number> values = (List<Number>) d.get("values");
        StringBuilder sb = new StringBuilder("公司整体能力画像：\n");
        for (int i = 0; i < indicators.size(); i++) sb.append("· ").append(indicators.get(i)).append(" ").append(values.get(i)).append("\n");
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("type", "radar");
        chart.put("title", "公司能力雷达");
        chart.put("indicators", indicators);
        chart.put("values", values.stream().map(Number::doubleValue).toList());
        return AssistantReply.withChart("RADAR", "公司能力雷达", sb.toString().trim(), chart,
                List.of("各部门多少人？", "平均薪资多少？", "招聘漏斗怎样？"));
    }

    // ==================== 个人数据（员工自助）====================

    private AssistantReply personal(String q, LoginUser user) {
        Long empId = user.employeeId();
        if (empId == null) {
            return AssistantReply.text("PERSONAL",
                    "你的账号尚未关联员工档案，暂时无法查询个人数据。可先向 HR 确认账号绑定。",
                    baseSuggestions());
        }
        if (containsAny(q, "考勤", "出勤")) return myAttendance(empId);
        if (containsAny(q, "绩效", "评分", "考核")) return myPerformance(empId);
        if (containsAny(q, "工资", "薪资", "薪酬", "到手")) return mySalary(empId);
        if (containsAny(q, "培训", "学习")) return myTraining(empId);
        return myOverview(empId);
    }

    private AssistantReply myOverview(Long empId) {
        Employee e = employeeRepository.findById(empId).orElse(null);
        if (e == null) return AssistantReply.text("PERSONAL", "未找到你的员工档案。", baseSuggestions());
        double att = myAttendanceRate(empId);
        PerformanceRecord p = performanceRepository.findByEmployeeIdOrderByPeriodDesc(empId).stream().findFirst().orElse(null);
        String reply = String.format("你好，%s 👋 你在「%s」担任「%s」，职级 %s，入职于 %s。\n近 3 个月出勤率 **%.1f%%**，最近一次绩效 **%s**。",
                e.getName(), deptName(e), posName(e), e.getLevel() == null ? "-" : e.getLevel(),
                e.getHireDate() == null ? "-" : e.getHireDate().toString(),
                att, p == null ? "暂无记录" : p.getRating() + "（" + p.getScore() + " 分）");
        return AssistantReply.text("PERSONAL", reply,
                List.of("我的考勤", "我的绩效", "我的工资", "我的培训"));
    }

    private AssistantReply myAttendance(Long empId) {
        double att = myAttendanceRate(empId);
        return AssistantReply.text("PERSONAL",
                String.format("你近 3 个月出勤率 **%.1f%%**，继续保持 👍 也可以去「考勤排班」查看月度详情。", att),
                List.of("我的绩效", "我的工资", "我的培训"));
    }

    private AssistantReply myPerformance(Long empId) {
        List<PerformanceRecord> list = performanceRepository.findByEmployeeIdOrderByPeriodDesc(empId);
        if (list.isEmpty()) return AssistantReply.text("PERSONAL", "你还没有绩效记录。", List.of("我的考勤", "我的工资"));
        PerformanceRecord p = list.get(0);
        return AssistantReply.text("PERSONAL",
                String.format("你最近一期（%s）绩效评级 **%s**，得分 **%s** 分。", p.getPeriod(), p.getRating(), p.getScore()),
                List.of("我的考勤", "我的工资", "我的培训"));
    }

    private AssistantReply mySalary(Long empId) {
        String month = YearMonth.now().format(MONTH_FMT);
        PayrollRecord p = payrollRepository.findByEmployeeIdAndPayMonth(empId, month).orElse(null);
        if (p == null) return AssistantReply.text("PERSONAL",
                String.format("本月（%s）工资条尚未生成，可到「薪酬福利」查看历史工资条。", month),
                List.of("我的考勤", "我的绩效"));
        return AssistantReply.text("PERSONAL",
                String.format("你 %s 的工资条：应发 **%s** 元，扣减 **%s** 元，实发 **%s** 元。",
                        month, fmtMoney(p.getGrossAmount() == null ? 0 : p.getGrossAmount().doubleValue()),
                        fmtMoney(p.getDeductionTotal() == null ? 0 : p.getDeductionTotal().doubleValue()),
                        fmtMoney(p.getNetAmount() == null ? 0 : p.getNetAmount().doubleValue())),
                List.of("我的考勤", "我的绩效", "我的培训"));
    }

    private AssistantReply myTraining(Long empId) {
        List<TrainingRecord> list = trainingRecordRepository.findByEmployeeIdOrderByEnrolledAtDesc(empId);
        long done = list.stream().filter(r -> "已完成".equals(r.getStatus())).count();
        return AssistantReply.text("PERSONAL",
                String.format("你累计参加培训 **%d** 门课程，其中已完成 **%d** 门。", list.size(), done),
                List.of("我的考勤", "我的绩效", "我的工资"));
    }

    private double myAttendanceRate(Long empId) {
        List<AttendanceRecord> recs = attendanceRepository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(empId, LocalDate.now().minusMonths(3), LocalDate.now());
        if (recs.isEmpty()) return 100;
        long abnormal = recs.stream().filter(r -> "ABSENT".equals(r.getStatus()) || "LATE".equals(r.getStatus())).count();
        return round1(100 - abnormal * 100.0 / recs.size());
    }

    // ==================== 图表与表格构建 ====================

    private Map<String, Object> barChart(String title, List<String> categories, List<List<String>> seriesNames, List<List<Double>> seriesData) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "bar");
        m.put("title", title);
        m.put("categories", categories);
        List<Map<String, Object>> series = new ArrayList<>();
        for (int i = 0; i < seriesNames.size(); i++) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("name", seriesNames.get(i).get(0));
            s.put("type", "bar");
            s.put("data", seriesData.get(i));
            series.add(s);
        }
        m.put("series", series);
        return m;
    }

    private Map<String, Object> lineChart(List<String> categories, List<String> seriesNames, List<List<Double>> seriesData) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "line");
        m.put("title", "趋势图");
        m.put("categories", categories);
        List<Map<String, Object>> series = new ArrayList<>();
        for (int i = 0; i < seriesNames.size(); i++) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("name", seriesNames.get(i));
            s.put("type", "line");
            s.put("data", seriesData.get(i));
            series.add(s);
        }
        m.put("series", series);
        return m;
    }

    private Map<String, Object> pieChart(List<String> names, List<Double> values) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "pie");
        m.put("title", "占比分布");
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) items.add(Map.of("name", names.get(i), "value", values.get(i)));
        m.put("items", items);
        return m;
    }

    private Map<String, Object> funnelChart(List<String> stages, List<Double> values) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "funnel");
        m.put("title", "招聘转化漏斗");
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < stages.size(); i++) items.add(Map.of("name", stages.get(i), "value", values.get(i)));
        m.put("items", items);
        return m;
    }

    private List<Map<String, Object>> buildNineBoxRows(List<Map<String, Object>> cells) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String perf : List.of("高", "中", "低")) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("perf", perf);
            for (String pot : List.of("低", "中", "高")) {
                String key = "低".equals(pot) ? "low" : "中".equals(pot) ? "mid" : "high";
                long v = cells.stream().filter(c -> pot.equals(c.get("potential")) && perf.equals(c.get("performance")))
                        .mapToLong(c -> ((Number) c.get("value")).longValue()).sum();
                row.put(key, v);
            }
            rows.add(row);
        }
        return rows;
    }

    // ==================== 会话历史 ====================

    public List<Map<String, Object>> history() {
        String username = SecurityUtil.currentUser().username();
        ArrayDeque<Map<String, Object>> deque = histories.get(username);
        return deque == null ? List.of() : new ArrayList<>(deque);
    }

    public void clearHistory() {
        String username = SecurityUtil.currentUser().username();
        histories.remove(username);
        lastIntent.remove(username);
    }

    private void recordChat(String username, String role, String content) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        msg.put("time", System.currentTimeMillis());
        addToHistory(username, msg);
    }

    /** 助手消息额外携带完整回复体（含图表/表格），前端刷新后仍可还原 */
    private void recordAssistantChat(String username, AssistantReply reply) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "assistant");
        msg.put("content", reply.reply());
        msg.put("reply", reply);
        msg.put("time", System.currentTimeMillis());
        addToHistory(username, msg);
    }

    private void addToHistory(String username, Map<String, Object> msg) {
        histories.computeIfAbsent(username, k -> new ArrayDeque<>()).addLast(msg);
        ArrayDeque<Map<String, Object>> deque = histories.get(username);
        while (deque.size() > HISTORY_LIMIT) deque.removeFirst();
        historyLastAccess.put(username, System.currentTimeMillis());
        // 全局上限：超过阈值时淘汰最久未访问的用户会话，避免用户名集合无界增长
        if (histories.size() > HISTORY_USER_LIMIT) {
            String victim = historyLastAccess.entrySet().stream()
                    .min(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
            if (victim != null && !victim.equals(username)) {
                histories.remove(victim);
                historyLastAccess.remove(victim);
                lastIntent.remove(victim);
            }
        }
    }

    // ==================== 工具 ====================

    private boolean canSeeFinancial(LoginUser user) {
        return !"EMPLOYEE".equals(user.role());
    }

    private boolean containsAny(String q, String... keys) {
        for (String k : keys) if (q.contains(k)) return true;
        return false;
    }

    private String normalize(String q) {
        if (q == null) return "";
        return q.replaceAll("[\\s　]", "").toLowerCase().trim();
    }

    private String extractDept(String q) {
        for (String n : DEPT_NAMES) if (q.contains(n)) return n;
        return null;
    }

    private String dimLabel(String dim) {
        return switch (dim) {
            case "education" -> "学历";
            case "level" -> "职级";
            case "gender" -> "性别";
            case "tenure" -> "司龄";
            default -> "年龄";
        };
    }

    private String deptName(Employee e) {
        return e.getDepartment() == null ? "-" : e.getDepartment().getName();
    }

    private String posName(Employee e) {
        return e.getPosition() == null ? "-" : e.getPosition().getName();
    }

    private long lastVal(List<Number> list) {
        return list.isEmpty() ? 0 : list.get(list.size() - 1).longValue();
    }

    private String fmtMoney(double v) {
        if (v >= 10000) return (Math.round(v / 1000) / 10.0) + "万";
        return String.valueOf(Math.round(v));
    }

    private String fmtWan(double v) {
        return String.valueOf(Math.round(v / 10000.0 * 10) / 10.0);
    }

    private double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private List<String> baseSuggestions() {
        return List.of("现在在职多少人？", "各部门人数？", "平均薪资多少？", "招聘漏斗怎样？", "我的考勤怎么样？");
    }
}
