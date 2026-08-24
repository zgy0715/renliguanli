package com.hrms.service;

import com.hrms.domain.attendance.AttendanceRecord;
import com.hrms.domain.employee.Employee;
import com.hrms.domain.recruitment.Candidate;
import com.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final PerformanceRecordRepository performanceRepository;
    private final CandidateRepository candidateRepository;
    private final PayrollRecordRepository payrollRepository;
    private final TrainingCourseRepository courseRepository;
    private final TrainingRecordRepository trainingRecordRepository;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /** 人力结构分布：age/education/level/gender/tenure */
    @Transactional(readOnly = true)
    public Map<String, Object> structure(String dimension) {
        List<Employee> all = employeeRepository.findAll().stream()
                .filter(e -> !"LEFT".equals(e.getStatus()))
                .collect(Collectors.toList());
        Map<String, Long> countMap = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();
        for (Employee e : all) {
            String key = switch (dimension) {
                case "age" -> {
                    int age = e.getBirthDate() == null ? 30 : (int) java.time.temporal.ChronoUnit.YEARS.between(e.getBirthDate(), now);
                    yield age < 25 ? "25岁以下" : age < 30 ? "25-29岁" : age < 35 ? "30-34岁" : age < 40 ? "35-39岁" : age < 45 ? "40-44岁" : "45岁以上";
                }
                case "education" -> e.getEducation() == null ? "未知" : e.getEducation();
                case "level" -> {
                    String lv = e.getLevel() == null ? "P2" : e.getLevel();
                    yield lv.startsWith("M") ? "管理序列(" + lv + ")" : "专业序列(" + lv + ")";
                }
                case "gender" -> "MALE".equals(e.getGender()) ? "男" : "女";
                case "tenure" -> {
                    int years = e.getHireDate() == null ? 0 : (int) java.time.temporal.ChronoUnit.YEARS.between(e.getHireDate(), now);
                    yield years < 1 ? "1年以下" : years < 3 ? "1-3年" : years < 5 ? "3-5年" : years < 8 ? "5-8年" : "8年以上";
                }
                default -> "-";
            };
            countMap.merge(key, 1L, Long::sum);
        }
        // 固定排序
        if ("age".equals(dimension) || "tenure".equals(dimension)) {
            countMap = countMap.entrySet().stream()
                    .sorted(Comparator.comparingInt(en -> keyIndex(en.getKey())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        }
        List<Map<String, Object>> items = countMap.entrySet().stream()
                .map(en -> Map.<String, Object>of("name", en.getKey(), "value", en.getValue()))
                .collect(Collectors.toList());
        return Map.of("dimension", dimension, "items", items);
    }

    /** 部门人数（横向柱状） */
    @Transactional(readOnly = true)
    public Map<String, Object> deptHeadcount() {
        List<Employee> all = employeeRepository.findAll().stream()
                .filter(e -> !"LEFT".equals(e.getStatus()) && e.getDepartment() != null)
                .collect(Collectors.toList());
        Map<String, Long> map = all.stream()
                .collect(Collectors.groupingBy(e -> e.getDepartment().getName(), LinkedHashMap::new, Collectors.counting()));
        List<Map<String, Object>> items = map.entrySet().stream()
                .map(en -> Map.<String, Object>of("name", en.getKey(), "value", en.getValue()))
                .collect(Collectors.toList());
        return Map.of("items", items);
    }

    /** 近12个月离职趋势 + 离职率 */
    @Transactional(readOnly = true)
    public Map<String, Object> attritionTrend() {
        List<Employee> all = employeeRepository.findAll();
        List<String> months = new ArrayList<>();
        List<Long> leaves = new ArrayList<>();
        List<Double> rates = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            months.add(ym.format(MONTH_FMT));
            LocalDate monthEnd = ym.atEndOfMonth();
            LocalDate monthStart = ym.atDay(1);
            long leave = all.stream().filter(e -> e.getLeaveDate() != null
                    && !e.getLeaveDate().isBefore(monthStart) && !e.getLeaveDate().isAfter(monthEnd)).count();
            long hc = all.stream().filter(e -> (e.getHireDate() != null && !e.getHireDate().isAfter(monthEnd))
                    && (e.getLeaveDate() == null || e.getLeaveDate().isAfter(monthEnd))).count();
            leaves.add(leave);
            rates.add(round1(hc > 0 ? leave * 100.0 / hc : 0));
        }
        return Map.of("months", months, "leaves", leaves, "rates", rates);
    }

    /** 人才盘点九宫格 */
    @Transactional(readOnly = true)
    public Map<String, Object> nineBox() {
        List<Employee> all = employeeRepository.findAll().stream()
                .filter(e -> !"LEFT".equals(e.getStatus()) && e.getGridCell() != null)
                .collect(Collectors.toList());
        // 行=潜力(1-9 低-高)，列=绩效(1-9 低-高)
        List<Map<String, Object>> cells = new ArrayList<>();
        int[][] grid = new int[3][3];
        String[] labels = {"低", "中", "高"};
        for (Employee e : all) {
            // gridCell 形如 "9A"：首位为潜力分数(1-9)，次位为绩效评级(A/B/C)
            String cell = e.getGridCell();
            int pot = Character.digit(cell.charAt(0), 10);
            char perfChar = cell.charAt(1);
            int potIdx = pot >= 9 ? 2 : pot >= 6 ? 1 : 0;
            int perfIdx = (perfChar == 'A' || perfChar == 'S') ? 2 : (perfChar == 'B') ? 1 : 0;
            grid[potIdx][perfIdx]++;
        }
        for (int pot = 0; pot < 3; pot++) {
            for (int perf = 0; perf < 3; perf++) {
                Map<String, Object> cell = new LinkedHashMap<>();
                cell.put("potential", labels[pot]);
                cell.put("performance", labels[perf]);
                cell.put("value", grid[pot][perf]);
                cells.add(cell);
            }
        }
        return Map.of("cells", cells, "total", all.size());
    }

    /** 招聘漏斗 */
    @Transactional(readOnly = true)
    public Map<String, Object> recruitmentFunnel() {
        List<Candidate> all = candidateRepository.findAll();
        String[] stages = {"简历筛选", "初试", "复试", "HR面", "Offer", "入职"};
        List<Map<String, Object>> funnel = new ArrayList<>();
        for (String s : stages) {
            long n = all.stream().filter(c -> isAtOrBeyond(c, s)).count();
            funnel.add(Map.of("stage", s, "value", n));
        }
        long total = all.size();
        double conversion = total > 0 ? round1(((Number) funnel.get(funnel.size() - 1).get("value")).doubleValue() * 100.0 / total) : 0;
        return Map.of("funnel", funnel, "total", total, "conversion", conversion);
    }

    /** 招聘渠道效能 */
    @Transactional(readOnly = true)
    public Map<String, Object> recruitmentChannel() {
        List<Candidate> all = candidateRepository.findAll();
        Map<String, Long> totalMap = all.stream()
                .collect(Collectors.groupingBy(Candidate::getChannel, LinkedHashMap::new, Collectors.counting()));
        // 与漏斗口径一致：仅「入职」算已招（不再把待 Offer 计为已招）
        Map<String, Long> hiredMap = all.stream()
                .filter(c -> "入职".equals(c.getStatus()))
                .collect(Collectors.groupingBy(Candidate::getChannel, LinkedHashMap::new, Collectors.counting()));
        List<Map<String, Object>> items = new ArrayList<>();
        for (var en : totalMap.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("channel", en.getKey());
            m.put("total", en.getValue());
            m.put("hired", hiredMap.getOrDefault(en.getKey(), 0L));
            m.put("rate", round1(hiredMap.getOrDefault(en.getKey(), 0L) * 100.0 / en.getValue()));
            items.add(m);
        }
        return Map.of("items", items);
    }

    /** 近6个月出勤率趋势 */
    @Transactional(readOnly = true)
    public Map<String, Object> attendanceTrend() {
        List<String> months = new ArrayList<>();
        List<Double> rates = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            months.add(ym.format(MONTH_FMT));
            LocalDate first = ym.atDay(1);
            LocalDate last = ym.atEndOfMonth();
            if (last.isAfter(LocalDate.now())) last = LocalDate.now();
            List<AttendanceRecord> recs = attendanceRepository.findByWorkDateBetweenOrderByWorkDateAsc(first, last);
            long total = recs.size();
            long abnormal = recs.stream().filter(r -> "ABSENT".equals(r.getStatus()) || "LATE".equals(r.getStatus())).count();
            rates.add(round1(total > 0 ? 100 - abnormal * 100.0 / total : 100));
        }
        return Map.of("months", months, "rates", rates);
    }

    /** 绩效分布 */
    @Transactional(readOnly = true)
    public Map<String, Object> performanceDistribution(String period) {
        List<com.hrms.domain.performance.PerformanceRecord> records = performanceRepository.findByPeriod(period);
        Map<String, Long> dist = records.stream()
                .collect(Collectors.groupingBy(r -> r.getRating() == null ? "B" : r.getRating(), LinkedHashMap::new, Collectors.counting()));
        List<Map<String, Object>> items = dist.entrySet().stream()
                .map(en -> Map.<String, Object>of("name", en.getKey(), "value", en.getValue()))
                .collect(Collectors.toList());
        return Map.of("period", period, "items", items);
    }

    /** 部门人效对比：人数/平均薪资/离职率/平均绩效 */
    @Transactional(readOnly = true)
    public Map<String, Object> deptEfficiency() {
        List<Employee> all = employeeRepository.findAll();
        Map<Long, List<Employee>> byDept = all.stream()
                .filter(e -> e.getDepartment() != null)
                .collect(Collectors.groupingBy(e -> e.getDepartment().getId()));
        List<Map<String, Object>> items = new ArrayList<>();
        for (var en : byDept.entrySet()) {
            List<Employee> list = en.getValue();
            String deptName = list.get(0).getDepartment().getName();
            long active = list.stream().filter(e -> !"LEFT".equals(e.getStatus())).count();
            double avgSalary = list.stream()
                    .filter(e -> "ACTIVE".equals(e.getStatus()) && e.getBaseSalary() != null)
                    .mapToDouble(e -> e.getBaseSalary().doubleValue()).average().orElse(0);
            long leave = list.stream().filter(e -> "LEFT".equals(e.getStatus())).count();
            double attrition = active + leave > 0 ? round1(leave * 100.0 / (active + leave)) : 0;
            items.add(Map.of(
                    "name", deptName,
                    "headcount", active,
                    "avgSalary", Math.round(avgSalary),
                    "attrition", attrition));
        }
        return Map.of("items", items);
    }

    /** 培训覆盖统计 */
    @Transactional(readOnly = true)
    public Map<String, Object> trainingStats() {
        long courses = courseRepository.count();
        long completed = trainingRecordRepository.countByStatus("已完成");
        long totalRecords = trainingRecordRepository.findAll().size();
        long activeEmp = employeeRepository.countByStatus("ACTIVE");
        // 覆盖率 = 至少完成一门课程的员工数 / 在职员工数（此前为记录数/人数，可能超过 100%）
        long coveredEmp = trainingRecordRepository.countDistinctEmployeeIdByStatus("已完成");
        double coverage = activeEmp > 0 ? round1(coveredEmp * 100.0 / activeEmp) : 0;
        return Map.of("courses", courses, "completed", completed, "totalRecords", totalRecords, "coverage", coverage);
    }

    /** 薪酬近6个月成本趋势 */
    @Transactional(readOnly = true)
    public Map<String, Object> payrollCostTrend() {
        YearMonth now = YearMonth.now();
        List<String> months = new ArrayList<>();
        List<Long> costs = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            String month = now.minusMonths(i).format(MONTH_FMT);
            months.add(month);
            long total = payrollRepository.findByPayMonth(month).stream()
                    .mapToLong(r -> r.getGrossAmount() == null ? 0 : r.getGrossAmount().longValue())
                    .sum();
            costs.add(total);
        }
        return Map.of("months", months, "costs", costs);
    }

    /** 员工能力雷达分布（按部门取平均绩效/出勤/培训/经验/薪资竞争力） */
    @Transactional(readOnly = true)
    public Map<String, Object> capabilityRadar(String deptName) {
        List<Employee> allActive = employeeRepository.findAll().stream()
                .filter(e -> !"LEFT".equals(e.getStatus()))
                .collect(Collectors.toList());
        // "全公司" 或空值 → 全员；否则精确匹配部门名
        boolean isAll = deptName == null || deptName.isBlank() || "全公司".equals(deptName);
        List<Employee> list = isAll ? allActive : allActive.stream()
                .filter(e -> e.getDepartment() != null && deptName.equals(e.getDepartment().getName()))
                .collect(Collectors.toList());
        if (list.isEmpty() && !isAll) list = allActive;

        double perf = list.stream()
                .map(e -> performanceRepository.findByEmployeeIdOrderByPeriodDesc(e.getId()))
                .filter(l -> !l.isEmpty())
                .mapToDouble(l -> l.get(0).getScore() == null ? 70 : l.get(0).getScore())
                .average().orElse(70);
        double tenure = list.stream().mapToDouble(e -> {
            if (e.getHireDate() == null) return 0;
            return Math.min(10, java.time.temporal.ChronoUnit.MONTHS.between(e.getHireDate(), LocalDate.now()) / 12.0);
        }).average().orElse(0);
        double avgSalary = list.stream().filter(e -> e.getBaseSalary() != null)
                .mapToDouble(e -> e.getBaseSalary().doubleValue()).average().orElse(0);
        // 薪资竞争力 = 部门均薪 / 公司均薪 × 100（相对值，随部门变化更明显）
        double companyAvg = allActive.stream().filter(e -> e.getBaseSalary() != null)
                .mapToDouble(e -> e.getBaseSalary().doubleValue()).average().orElse(30000);
        double salaryIndex = Math.min(100, Math.max(60, avgSalary / companyAvg * 100));
        double training = list.stream().mapToLong(e -> trainingRecordRepository.findByEmployeeIdOrderByEnrolledAtDesc(e.getId()).stream()
                .filter(r -> "已完成".equals(r.getStatus())).count()).average().orElse(0);
        double trainingIndex = Math.min(100, training / 5 * 100);

        // 出勤稳定：近 3 个月真实考勤中非异常记录占比（异常 = 迟到 / 缺勤）
        List<AttendanceRecord> recs = attendanceRepository.findByWorkDateBetweenOrderByWorkDateAsc(
                LocalDate.now().minusMonths(3), LocalDate.now());
        Map<Long, List<AttendanceRecord>> byEmp = recs.stream()
                .filter(r -> r.getEmployeeId() != null)
                .collect(Collectors.groupingBy(AttendanceRecord::getEmployeeId));
        double attendance = list.stream()
                .filter(e -> byEmp.containsKey(e.getId()))
                .mapToDouble(e -> {
                    List<AttendanceRecord> er = byEmp.get(e.getId());
                    long total = er.size();
                    long abnormal = er.stream()
                            .filter(r -> "ABSENT".equals(r.getStatus()) || "LATE".equals(r.getStatus()))
                            .count();
                    return total > 0 ? 100 - abnormal * 100.0 / total : 100;
                })
                .average().orElse(100);
        attendance = round1(attendance);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dept", deptName);
        result.put("indicators", List.of("绩效", "经验", "薪资竞争力", "培训深度", "出勤稳定", "技能覆盖"));
        result.put("values", List.of(round1(perf), round1(Math.min(100, tenure * 10)), round1(salaryIndex),
                round1(trainingIndex), attendance, round1((perf + trainingIndex) / 2)));
        return result;
    }

    private boolean isAtOrBeyond(Candidate c, String stage) {
        String s = c.getStatus();
        if (s == null) return false;
        List<String> order = List.of("简历筛选", "初试", "复试", "HR面", "Offer", "入职");
        if ("淘汰".equals(s)) return stage.equals("简历筛选");
        int a = order.indexOf(stage);
        int b = order.indexOf(s);
        return a >= 0 && b >= 0 && b >= a;
    }

    private int keyIndex(String key) {
        String[] order = {"25岁以下", "25-29岁", "30-34岁", "35-39岁", "40-44岁", "45岁以上",
                "1年以下", "1-3年", "3-5年", "5-8年", "8年以上"};
        for (int i = 0; i < order.length; i++) if (order[i].equals(key)) return i;
        return 99;
    }

    private double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
