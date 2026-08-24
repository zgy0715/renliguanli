package com.hrms.config;

import com.hrms.domain.attendance.AttendanceRecord;
import com.hrms.domain.attendance.LeaveRequest;
import com.hrms.domain.attendance.Shift;
import com.hrms.domain.employee.Contract;
import com.hrms.domain.employee.Education;
import com.hrms.domain.employee.Employee;
import com.hrms.domain.employee.Transfer;
import com.hrms.domain.hr.Announcement;
import com.hrms.domain.hr.Certificate;
import com.hrms.domain.hr.Todo;
import com.hrms.domain.org.Department;
import com.hrms.domain.org.Position;
import com.hrms.domain.payroll.PayrollRecord;
import com.hrms.domain.performance.PerformanceRecord;
import com.hrms.domain.recruitment.Candidate;
import com.hrms.domain.recruitment.JobRequisition;
import com.hrms.domain.system.DictItem;
import com.hrms.domain.system.OperationLog;
import com.hrms.domain.system.User;
import com.hrms.domain.training.TrainingCourse;
import com.hrms.domain.training.TrainingRecord;
import com.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 演示数据播种器：生成一整套真实感十足的企业数据，保证可视化图表饱满
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final EmployeeRepository employeeRepository;
    private final ContractRepository contractRepository;
    private final EducationRepository educationRepository;
    private final TransferRepository transferRepository;
    private final ShiftRepository shiftRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final LeaveRequestRepository leaveRepository;
    private final PayrollRecordRepository payrollRepository;
    private final PerformanceRecordRepository performanceRepository;
    private final TrainingCourseRepository courseRepository;
    private final TrainingRecordRepository trainingRecordRepository;
    private final CandidateRepository candidateRepository;
    private final JobRequisitionRepository jobRepository;
    private final AnnouncementRepository announcementRepository;
    private final TodoRepository todoRepository;
    private final CertificateRepository certificateRepository;
    private final OperationLogRepository operationLogRepository;
    private final DictItemRepository dictRepository;

    private final Random random = new Random(42);

    // ========== 姓名库 ==========
    private static final String[] SURNAMES = {
            "王", "李", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴",
            "徐", "孙", "胡", "朱", "高", "林", "何", "郭", "马", "罗",
            "梁", "宋", "郑", "谢", "韩", "唐", "冯", "于", "董", "萧",
            "程", "曹", "袁", "邓", "许", "傅", "沈", "曾", "彭", "吕",
            "苏", "卢", "蒋", "蔡", "贾", "丁", "魏", "薛", "叶", "阎",
            "余", "潘", "杜", "戴", "夏", "钟", "汪", "田", "任", "姜"};
    private static final String[] GIVEN_MALE = {
            "伟", "强", "磊", "军", "洋", "勇", "杰", "涛", "明", "超",
            "刚", "平", "辉", "鹏", "飞", "旭", "宁", "凯", "浩", "鑫",
            "宇", "晨", "俊", "帆", "斌", "龙", "锋", "波", "东", "昊",
            "翔", "然", "泽", "睿", "博", "文", "辉", "康", "兴", "亮"};
    private static final String[] GIVEN_FEMALE = {
            "芳", "娜", "敏", "静", "丽", "燕", "玲", "红", "梅", "琳",
            "娟", "婷", "慧", "雪", "欣", "妍", "颖", "瑶", "琪", "雨",
            "思", "嘉", "怡", "梦", "璐", "璐", "茜", "蕾", "彤", "璇",
            "萍", "虹", "丹", "雯", "莉", "倩", "凤", "晶", "悦", "菲"};
    private static final String[] SCHOOLS = {
            "清华大学", "北京大学", "浙江大学", "上海交通大学", "复旦大学", "南京大学",
            "华中科技大学", "武汉大学", "西安交通大学", "同济大学", "中山大学", "哈尔滨工业大学",
            "北京邮电大学", "电子科技大学", "东南大学", "华南理工大学", "重庆大学", "天津大学",
            "四川大学", "山东大学", "厦门大学", "北京理工大学", "湖南大学", "东北大学"};
    private static final String[] MAJORS = {
            "计算机科学与技术", "软件工程", "信息管理与信息系统", "电子信息工程", "通信工程",
            "自动化", "数学与应用数学", "市场营销", "工商管理", "会计学", "人力资源管理",
            "金融学", "心理学", "设计学", "汉语言文学", "法学", "统计学", "电子商务"};

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("演示数据已存在，跳过播种");
            return;
        }
        long t0 = System.currentTimeMillis();

        seedDict();
        List<Department> depts = seedDepartments();
        Map<Long, Department> deptMap = depts.stream().collect(Collectors.toMap(Department::getId, d -> d));
        List<Position> positions = seedPositions(deptMap);
        List<Employee> employees = seedEmployees(deptMap, positions);
        Map<Long, Employee> empMap = employees.stream().collect(Collectors.toMap(Employee::getId, e -> e));
        seedUsers(empMap);
        seedContracts(employees);
        seedEducation(employees);
        seedTransfers(employees, empMap);
        seedShifts();
        List<LeaveRequest> leaves = seedLeaves(employees);
        seedAttendance(employees);
        seedPayroll(employees);
        seedPerformance(employees);
        seedTraining(employees);
        seedRecruitment(deptMap);
        seedAnnouncements();
        seedTodos(employees);
        seedCertificates(employees);
        seedOperationLogs();

        long cost = System.currentTimeMillis() - t0;
        log.info("=== 演示数据播种完成：{} 员工 / {} 部门 / {} 考勤记录 / {} 薪酬记录，耗时 {}ms ===",
                employees.size(), depts.size(), attendanceRepository.count(), payrollRepository.count(), cost);
    }

    // ================== 数据字典 ==================
    private void seedDict() {
        String[][] dicts = {
                {"gender", "男", "MALE"}, {"gender", "女", "FEMALE"},
                {"education", "博士", "博士"}, {"education", "硕士", "硕士"}, {"education", "本科", "本科"},
                {"education", "大专", "大专"}, {"education", "高中", "高中"},
                {"emp_status", "在职", "ACTIVE"}, {"emp_status", "试用", "PROBATION"},
                {"emp_status", "离职", "LEFT"}, {"emp_status", "停职", "SUSPEND"},
                {"att_status", "正常", "NORMAL"}, {"att_status", "迟到", "LATE"}, {"att_status", "早退", "EARLY"},
                {"att_status", "缺勤", "ABSENT"}, {"att_status", "请假", "LEAVE"}, {"att_status", "外勤", "FIELD"},
                {"leave_type", "年假", "年假"}, {"leave_type", "事假", "事假"}, {"leave_type", "病假", "病假"},
                {"leave_type", "婚假", "婚假"}, {"leave_type", "产假", "产假"}, {"leave_type", "调休", "调休"},
                {"leave_type", "加班", "加班"},
                {"pay_status", "草稿", "DRAFT"}, {"pay_status", "已核算", "CALCULATED"},
                {"pay_status", "已确认", "CONFIRMED"}, {"pay_status", "已发放", "PAID"},
                {"contract_type", "固定期限", "固定期限"}, {"contract_type", "无固定期限", "无固定期限"},
                {"contract_type", "实习", "实习"}, {"contract_type", "劳务", "劳务"},
                {"contract_status", "有效", "有效"}, {"contract_status", "即将到期", "即将到期"},
                {"contract_status", "已到期", "已到期"}, {"contract_status", "已终止", "已终止"},
                {"perf_rating", "S 卓越", "S"}, {"perf_rating", "A 优秀", "A"}, {"perf_rating", "B 良好", "B"},
                {"perf_rating", "C 合格", "C"}, {"perf_rating", "D 待改进", "D"},
                {"recruit_status", "简历筛选", "简历筛选"}, {"recruit_status", "初试", "初试"},
                {"recruit_status", "复试", "复试"}, {"recruit_status", "HR面", "HR面"},
                {"recruit_status", "Offer", "Offer"}, {"recruit_status", "入职", "入职"},
                {"recruit_status", "淘汰", "淘汰"},
                {"todo_type", "审批", "审批"}, {"todo_type", "入职", "入职"}, {"todo_type", "转正", "转正"},
                {"todo_type", "合同到期", "合同到期"}, {"todo_type", "考勤异常", "考勤异常"},
                {"todo_type", "培训", "培训"},
                {"transfer_type", "调岗", "TRANSFER"}, {"transfer_type", "晋升", "PROMOTE"},
                {"transfer_type", "转正", "REGULAR"}, {"transfer_type", "调薪", "SALARY"},
                {"transfer_type", "离职", "LEAVE"},
        };
        List<DictItem> items = new ArrayList<>();
        for (String[] d : dicts) {
            DictItem item = new DictItem();
            item.setType(d[0]);
            item.setLabel(d[1]);
            item.setValue(d[2]);
            item.setSort(items.size());
            item.setStatus(1);
            items.add(item);
        }
        dictRepository.saveAll(items);
    }

    // ================== 组织架构 ==================
    private List<Department> seedDepartments() {
        Object[][] defs = {
                {"集团总部", null, "HQ", 4},
                {"技术中心", 1L, "TECH", 12},
                {"后端研发部", 2L, "TECH", 30},
                {"前端研发部", 2L, "TECH", 14},
                {"测试部", 2L, "TECH", 10},
                {"运维部", 2L, "TECH", 8},
                {"数据智能部", 2L, "TECH", 12},
                {"产品中心", 1L, "PRODUCT", 6},
                {"产品部", 8L, "PRODUCT", 12},
                {"设计部", 8L, "PRODUCT", 10},
                {"运营中心", 1L, "OPS", 5},
                {"运营部", 11L, "OPS", 10},
                {"市场部", 11L, "OPS", 8},
                {"客服部", 11L, "OPS", 12},
                {"销售中心", 1L, "SALES", 6},
                {"大客户销售部", 15L, "SALES", 14},
                {"渠道销售部", 15L, "SALES", 12},
                {"职能中心", 1L, "FUNCTION", 4},
                {"人力资源部", 18L, "FUNCTION", 8},
                {"财务部", 18L, "FUNCTION", 8},
                {"行政部", 18L, "FUNCTION", 6},
                {"法务部", 18L, "FUNCTION", 4},
        };
        List<Department> list = new ArrayList<>();
        long idCounter = 1;
        // 记录 (idIndex, name, parentId, category, headcount)
        List<Long> ids = new ArrayList<>();
        List<Department> all = new ArrayList<>();
        for (Object[] d : defs) {
            Department dep = new Department();
            dep.setName((String) d[0]);
            dep.setCode((String) d[2]);
            Long parent = (Long) d[1];
            dep.setParentId(parent == null ? null : ids.get(parent.intValue() - 1));
            dep.setSort((int) idCounter);
            dep.setStatus(1);
            dep.setCreatedAt(LocalDateTime.now());
            dep.setUpdatedAt(LocalDateTime.now());
            all.add(dep);
            ids.add(idCounter++);
        }
        departmentRepository.saveAll(all);
        return all;
    }

    private List<Position> seedPositions(Map<Long, Department> deptMap) {
        Object[][] defs = {
                // name, level, category, dept(1-index or null)
                {"CEO 首席执行官", "M5", "管理", null},
                {"CTO 首席技术官", "M5", "管理", null},
                {"CFO 首席财务官", "M5", "管理", null},
                {"副总裁", "M4", "管理", null},
                {"技术总监", "M3", "技术", 2L},
                {"架构师", "P7", "技术", null},
                {"技术专家", "P7", "技术", null},
                {"高级工程师", "P5", "技术", null},
                {"中级工程师", "P4", "技术", null},
                {"初级工程师", "P2", "技术", null},
                {"开发实习生", "P1", "技术", null},
                {"测试主管", "P5", "技术", null},
                {"测试工程师", "P3", "技术", null},
                {"运维主管", "P5", "技术", null},
                {"运维工程师", "P3", "技术", null},
                {"数据科学家", "P6", "技术", null},
                {"数据分析师", "P4", "技术", null},
                {"产品总监", "M3", "产品", 8L},
                {"高级产品经理", "P5", "产品", null},
                {"产品经理", "P4", "产品", null},
                {"产品助理", "P2", "产品", null},
                {"UI设计师", "P4", "产品", null},
                {"交互设计师", "P3", "产品", null},
                {"视觉设计师", "P3", "产品", null},
                {"运营总监", "M3", "运营", 11L},
                {"高级运营", "P4", "运营", null},
                {"运营专员", "P2", "运营", null},
                {"市场经理", "P4", "市场", null},
                {"市场专员", "P2", "市场", null},
                {"客服主管", "P3", "客服", null},
                {"客服专员", "P1", "客服", null},
                {"销售总监", "M4", "销售", 15L},
                {"大客户经理", "M2", "销售", null},
                {"销售经理", "P4", "销售", null},
                {"渠道经理", "P4", "销售", null},
                {"销售代表", "P2", "销售", null},
                {"职能总监", "M3", "职能", 18L},
                {"HR经理", "P4", "职能", null},
                {"HR专员", "P2", "职能", null},
                {"财务经理", "P4", "职能", null},
                {"会计", "P3", "职能", null},
                {"出纳", "P2", "职能", null},
                {"行政主管", "P3", "职能", null},
                {"行政专员", "P2", "职能", null},
                {"法务经理", "P4", "职能", null},
                {"法务专员", "P3", "职能", null},
        };
        List<Position> list = new ArrayList<>();
        for (Object[] p : defs) {
            Position pos = new Position();
            pos.setName((String) p[0]);
            pos.setLevel((String) p[1]);
            pos.setCategory((String) p[2]);
            pos.setDeptId((Long) p[3]);
            pos.setCode("P-" + (list.size() + 1));
            pos.setCreatedAt(LocalDateTime.now());
            pos.setUpdatedAt(LocalDateTime.now());
            list.add(pos);
        }
        positionRepository.saveAll(list);
        return list;
    }

    // ================== 员工 ==================
    private List<Employee> seedEmployees(Map<Long, Department> deptMap, List<Position> positions) {
        // 每个部门的编制（总编制 61，扣除离职后约 54 名在职，符合中型团队规模）
        long[][] headcount = {
                {1, 3},   // 集团总部 3 名高管
                {2, 3},   // 技术中心 3
                {3, 6},   // 后端 6
                {4, 4},   // 前端 4
                {5, 3},   // 测试 3
                {6, 2},   // 运维 2
                {7, 3},   // 数据 3
                {8, 2},   // 产品中心 2
                {9, 3},   // 产品 3
                {10, 3},  // 设计 3
                {11, 2},  // 运营中心 2
                {12, 3},  // 运营 3
                {13, 2},  // 市场 2
                {14, 3},  // 客服 3
                {15, 2},  // 销售中心 2
                {16, 4},  // 大客户 4
                {17, 3},  // 渠道 3
                {18, 2},  // 职能中心 2
                {19, 2},  // HR 2
                {20, 2},  // 财务 2
                {21, 2},  // 行政 2
                {22, 2},  // 法务 2
        };
        List<Employee> all = new ArrayList<>();
        int total = Arrays.stream(headcount).mapToInt(r -> (int) r[1]).sum();
        int empNo = 1001;
        for (long[] hc : headcount) {
            long deptId = hc[0];
            Department dept = deptMap.get(deptId);
            int count = (int) hc[1];
            for (int i = 0; i < count; i++) {
                Employee e = new Employee();
                e.setEmployeeNo("E" + empNo++);
                String gender = random.nextDouble() < 0.42 ? "FEMALE" : "MALE";
                e.setGender(gender);
                e.setName(genName(gender));
                int age = 22 + random.nextInt(25); // 22-46
                e.setBirthDate(LocalDate.now().minusYears(age).minusDays(random.nextInt(300)));
                e.setPhone("138" + (10000000 + random.nextInt(89999999)));
                e.setEmail(pinyinOrFallback(e.getName()) + "@hrms.com");
                e.setAddress(randomAddress());
                e.setIdCard("1101" + (198000000000L + random.nextLong(2000000000L)));
                e.setEducation(pickEducation(age));
                e.setSchool(SCHOOLS[random.nextInt(SCHOOLS.length)]);
                e.setMajor(MAJORS[random.nextInt(MAJORS.length)]);
                e.setMaritalStatus(random.nextDouble() < 0.4 ? "已婚" : "未婚");
                e.setPoliticalStatus(pickPolitical());
                e.setNationality("汉族");
                e.setNativePlace(SURNAMES[random.nextInt(SURNAMES.length)] + "省");

                e.setDepartment(dept);
                // 职位与职级
                Position pos = pickPosition(dept, positions, i, count);
                e.setPosition(pos);
                e.setLevel(pos.getLevel());
                // 入职时间
                int tenureYears = Math.min(age - 20, random.nextInt(7) + 1);
                LocalDate hire = LocalDate.now().minusYears(tenureYears).minusDays(random.nextInt(300));
                // 部分员工短期入职
                if (random.nextDouble() < 0.15) hire = LocalDate.now().minusDays(random.nextInt(90));
                e.setHireDate(hire);
                // 状态
                double statusRoll = random.nextDouble();
                if (statusRoll < 0.12) {
                    e.setStatus("LEFT");
                    e.setLeaveDate(hire.plusMonths(random.nextInt(36) + 3).plusDays(random.nextInt(20)));
                    if (e.getLeaveDate().isAfter(LocalDate.now())) e.setLeaveDate(LocalDate.now().minusDays(random.nextInt(120)));
                    e.setRegularDate(hire.plusMonths(3).plusDays(random.nextInt(10)));
                } else if (hire.plusMonths(3).isAfter(LocalDate.now()) && random.nextDouble() < 0.6) {
                    e.setStatus("PROBATION");
                    e.setRegularDate(hire.plusMonths(3).plusDays(random.nextInt(10)));
                } else {
                    e.setStatus("ACTIVE");
                    e.setRegularDate(hire.plusMonths(3).plusDays(random.nextInt(10)));
                }
                if (e.getRegularDate().isAfter(LocalDate.now())) e.setRegularDate(null);
                // 薪资（按职级）
                e.setBaseSalary(calcSalaryByLevel(e.getLevel()));
                e.setBankCard("6222" + (100000000000L + random.nextLong(900000000000L)));
                e.setEmergencyContact("家人");
                e.setEmergencyPhone("139" + (10000000 + random.nextInt(89999999)));
                // 人才标签与九宫格
                applyTalentProfile(e);
                e.setCreatedAt(LocalDateTime.now());
                e.setUpdatedAt(LocalDateTime.now());
                all.add(e);
            }
        }
        // 先持久化员工以获得 ID，再分配上级主管（managerId 依赖实体 ID，此前所有 managerId 均为 null）
        employeeRepository.saveAll(all);
        assignManagers(all, deptMap);
        // 二次保存以持久化 managerId
        employeeRepository.saveAll(all);
        return all;
    }

    private String pickEducation(int age) {
        double r = random.nextDouble();
        if (age >= 35) {
            if (r < 0.25) return "硕士";
            if (r < 0.55) return "本科";
            if (r < 0.8) return "大专";
            return "博士";
        }
        if (r < 0.35) return "硕士";
        if (r < 0.78) return "本科";
        if (r < 0.9) return "大专";
        return "博士";
    }

    private String pickPolitical() {
        double r = random.nextDouble();
        if (r < 0.2) return "中共党员";
        if (r < 0.3) return "共青团员";
        return "群众";
    }

    private Position pickPosition(Department dept, List<Position> positions, int idx, int count) {
        String cat = dept.getCode() != null ? dept.getCode() : "";
        List<Position> candidates = new ArrayList<>();
        if (cat.contains("TECH")) {
            if (idx == 0) candidates = filter(positions, "技术总监");
            else if (idx == 1 || idx == 2) candidates = filterAny(positions, "架构师", "技术专家");
            else if (idx < count * 0.35) candidates = filter(positions, "高级工程师");
            else if (idx < count * 0.75) candidates = filter(positions, "中级工程师");
            else if (idx < count * 0.95) candidates = filter(positions, "初级工程师");
            else candidates = filter(positions, "开发实习生");
        } else if (cat.contains("PRODUCT")) {
            if (idx == 0) candidates = filter(positions, "产品总监");
            else if (idx == 1 || idx == 2) candidates = filterAny(positions, "高级产品经理", "UI设计师");
            else if (idx < count * 0.6) candidates = filterAny(positions, "产品经理", "UI设计师", "交互设计师");
            else candidates = filterAny(positions, "产品助理", "视觉设计师");
        } else if (cat.contains("OPS")) {
            if (idx == 0) candidates = filter(positions, "运营总监");
            else if (idx == 1 || idx == 2) candidates = filterAny(positions, "高级运营", "市场经理");
            else if (idx < count * 0.6) candidates = filterAny(positions, "运营专员", "市场专员", "客服主管");
            else candidates = filter(positions, "客服专员");
        } else if (cat.contains("SALES")) {
            if (idx == 0) candidates = filter(positions, "销售总监");
            else if (idx == 1 || idx == 2) candidates = filterAny(positions, "大客户经理", "销售经理");
            else candidates = filterAny(positions, "渠道经理", "销售经理", "销售代表");
        } else if (cat.contains("FUNCTION")) {
            if (idx == 0) candidates = filter(positions, "职能总监");
            else if (dept.getName().contains("人力资源")) candidates = idx == 1 ? filter(positions, "HR经理") : filter(positions, "HR专员");
            else if (dept.getName().contains("财务")) candidates = idx == 1 ? filter(positions, "财务经理") : (idx == 2 ? filter(positions, "会计") : filter(positions, "出纳"));
            else if (dept.getName().contains("行政")) candidates = idx == 1 ? filter(positions, "行政主管") : filter(positions, "行政专员");
            else if (dept.getName().contains("法务")) candidates = idx == 1 ? filter(positions, "法务经理") : filter(positions, "法务专员");
            else candidates = filter(positions, "职能总监");
        } else {
            candidates = idx == 0 ? filterAny(positions, "副总裁", "CFO 首席财务官") : filterAny(positions, "CEO 首席执行官", "CTO 首席技术官", "CFO 首席财务官", "副总裁");
        }
        if (candidates.isEmpty()) return positions.get(random.nextInt(positions.size()));
        return candidates.get(random.nextInt(candidates.size()));
    }

    private List<Position> filter(List<Position> list, String name) {
        return list.stream().filter(p -> p.getName().equals(name)).collect(Collectors.toList());
    }

    private List<Position> filterAny(List<Position> list, String... names) {
        Set<String> set = Set.of(names);
        return list.stream().filter(p -> set.contains(p.getName())).collect(Collectors.toList());
    }

    private void assignManagers(List<Employee> employees, Map<Long, Department> deptMap) {
        Map<Long, Employee> leaderByDept = new HashMap<>();
        for (Employee e : employees) {
            if (e.getDepartment() != null && !leaderByDept.containsKey(e.getDepartment().getId())
                    && !"LEFT".equals(e.getStatus()) && !"PROBATION".equals(e.getStatus())) {
                leaderByDept.put(e.getDepartment().getId(), e);
            }
        }
        // 集团总部 leader = CEO
        for (Employee e : employees) {
            if (e.getDepartment() != null && e.getDepartment().getParentId() != null) {
                Long parentId = e.getDepartment().getParentId();
                Employee leader = leaderByDept.get(parentId);
                Employee deptLeader = leaderByDept.get(e.getDepartment().getId());
                if (!e.equals(deptLeader)) {
                    e.setManagerId(deptLeader != null ? deptLeader.getId() : (leader != null ? leader.getId() : null));
                }
            } else {
                e.setManagerId(null);
            }
        }
        // 中心 leader 的 manager 是集团高管
        for (Employee e : employees) {
            if (e.getDepartment() != null && e.getDepartment().getParentId() == null) {
                // 集团总部员工互为管理，CEO 无上级
                e.setManagerId(null);
            }
        }
    }

    private BigDecimal calcSalaryByLevel(String level) {
        double base;
        switch (level == null ? "P2" : level) {
            case "P1": base = 6000; break;
            case "P2": base = 9000; break;
            case "P3": base = 13000; break;
            case "P4": base = 17000; break;
            case "P5": base = 23000; break;
            case "P6": base = 30000; break;
            case "P7": base = 40000; break;
            case "P8": base = 55000; break;
            case "M1": base = 15000; break;
            case "M2": base = 21000; break;
            case "M3": base = 30000; break;
            case "M4": base = 42000; break;
            case "M5": base = 60000; break;
            default: base = 12000;
        }
        double variance = 0.9 + random.nextDouble() * 0.3;
        return BigDecimal.valueOf(base * variance).setScale(0, RoundingMode.HALF_UP);
    }

    private void applyTalentProfile(Employee e) {
        if ("LEFT".equals(e.getStatus())) return;
        double r = random.nextDouble();
        String cell;
        if (r < 0.08) { cell = "9A"; }        // 高潜高绩效
        else if (r < 0.16) { cell = "8A"; }
        else if (r < 0.26) { cell = "9B"; }
        else if (r < 0.38) { cell = "6B"; }
        else if (r < 0.52) { cell = "9C"; }
        else if (r < 0.66) { cell = "6C"; }
        else if (r < 0.80) { cell = "5C"; }
        else if (r < 0.92) { cell = "3C"; }
        else { cell = "1C"; }
        e.setGridCell(cell);
        // 标签
        List<String> tags = new ArrayList<>();
        if (cell.charAt(0) == '9' && cell.charAt(1) == 'A') tags.add("高潜");
        else if (cell.charAt(0) == '9' || cell.charAt(0) == '8') tags.add("核心骨干");
        if (cell.charAt(1) == 'C') tags.add("待改进");
        if (cell.charAt(0) == '6' || cell.charAt(0) == '9') tags.add("稳定贡献");
        if (random.nextDouble() < 0.2) tags.add("储备干部");
        if (random.nextDouble() < 0.15) tags.add("技术专家");
        if (random.nextDouble() < 0.1) tags.add("新人");
        e.setTags(String.join(",", tags));
        // 画像评分
        int base = switch (cell.charAt(0)) { case '9' -> 86; case '8' -> 80; case '6' -> 74; case '5' -> 66; case '3' -> 58; default -> 50; };
        int adj = cell.charAt(1) == 'A' ? 8 : cell.charAt(1) == 'B' ? 4 : -2;
        e.setProfileScore(Math.max(30, Math.min(98, base + adj + random.nextInt(6))));
    }

    // ================== 账号 ==================
    private void seedUsers(Map<Long, Employee> empMap) {
        // 找一个在职员工做自助账号
        Employee staff = empMap.values().stream()
                .filter(e -> "ACTIVE".equals(e.getStatus()))
                .findFirst().orElse(null);
        // 部门 leader 做 manager 账号
        List<Employee> managers = empMap.values().stream()
                .filter(e -> e.getManagerId() == null && !"LEFT".equals(e.getStatus()))
                .limit(3).collect(Collectors.toList());

        List<User> users = new ArrayList<>();
        users.add(buildUser("admin", "系统管理员", "ADMIN", null));
        users.add(buildUser("hr", "HR专员-小李", "HR", null));
        users.add(buildUser("manager1", "技术总监-王经理", "MANAGER", managers.isEmpty() ? null : managers.get(0).getId()));
        if (staff != null) users.add(buildUser("employee1", staff.getName(), "EMPLOYEE", staff.getId()));
        users.add(buildUser("manager2", "销售总监-张总", "MANAGER", managers.size() > 1 ? managers.get(1).getId() : null));
        userRepository.saveAll(users);
    }

    private User buildUser(String username, String displayName, String role, Long employeeId) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        u.setDisplayName(displayName);
        u.setRole(role);
        u.setEmployeeId(employeeId);
        u.setStatus(1);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        return u;
    }

    // ================== 合同 / 教育 / 异动 ==================
    private void seedContracts(List<Employee> employees) {
        List<Contract> list = new ArrayList<>();
        for (Employee e : employees) {
            Contract c = new Contract();
            c.setEmployeeId(e.getId());
            c.setType(random.nextDouble() < 0.3 ? "无固定期限" : "固定期限");
            c.setContractNo("HT-" + e.getEmployeeNo());
            c.setSignDate(e.getHireDate());
            c.setStartDate(e.getHireDate());
            if ("无固定期限".equals(c.getType())) {
                c.setEndDate(null);
                c.setStatus("有效");
            } else {
                c.setEndDate(e.getHireDate().plusYears(3));
                long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), c.getEndDate());
                if (days < 0) c.setStatus("已到期");
                else if (days < 60) c.setStatus("即将到期");
                else c.setStatus("有效");
            }
            c.setESigned(random.nextDouble() < 0.85);
            c.setCreatedAt(LocalDateTime.now());
            list.add(c);
        }
        contractRepository.saveAll(list);
    }

    private void seedEducation(List<Employee> employees) {
        List<Education> list = new ArrayList<>();
        for (Employee e : employees) {
            Education edu = new Education();
            edu.setEmployeeId(e.getId());
            edu.setDegree(e.getEducation());
            edu.setSchool(e.getSchool());
            edu.setMajor(e.getMajor());
            edu.setStartDate(e.getHireDate().minusYears(4));
            edu.setEndDate(e.getHireDate().minusYears(1));
            edu.setCreatedAt(LocalDateTime.now());
            list.add(edu);
        }
        educationRepository.saveAll(list);
    }

    private void seedTransfers(List<Employee> employees, Map<Long, Employee> empMap) {
        List<Transfer> list = new ArrayList<>();
        for (Employee e : employees) {
            if ("LEFT".equals(e.getStatus()) || random.nextDouble() > 0.3) continue;
            int n = random.nextInt(2) + 1;
            for (int i = 0; i < n; i++) {
                Transfer t = new Transfer();
                t.setEmployeeId(e.getId());
                t.setType(random.nextDouble() < 0.4 ? "PROMOTE" : (random.nextDouble() < 0.6 ? "TRANSFER" : "SALARY"));
                t.setFromDeptId(e.getDepartment() != null ? e.getDepartment().getId() : null);
                t.setToDeptId(e.getDepartment() != null ? e.getDepartment().getId() : null);
                t.setFromPositionId(e.getPosition() != null ? e.getPosition().getId() : null);
                t.setToPositionId(e.getPosition() != null ? e.getPosition().getId() : null);
                t.setFromLevel(e.getLevel());
                t.setToLevel(e.getLevel());
                t.setEffectiveDate(e.getHireDate().plusMonths((i + 1) * 12).plusDays(random.nextInt(20)));
                t.setReason("业务发展与人才梯队建设");
                t.setOperatorId(1L);
                t.setCreatedAt(LocalDateTime.now());
                list.add(t);
            }
        }
        transferRepository.saveAll(list);
    }

    // ================== 排班 ==================
    private void seedShifts() {
        Shift std = new Shift();
        std.setName("标准班");
        std.setType("标准");
        std.setWorkStart(LocalTime.of(9, 0));
        std.setWorkEnd(LocalTime.of(18, 0));
        std.setStandardHours(8);
        std.setWorkDays("1,2,3,4,5");
        std.setLateGraceMinutes(10);
        std.setFlexible(false);
        std.setStatus(1);
        std.setCreatedAt(LocalDateTime.now());

        Shift flex = new Shift();
        flex.setName("弹性班");
        flex.setType("弹性");
        flex.setWorkStart(LocalTime.of(9, 0));
        flex.setWorkEnd(LocalTime.of(18, 0));
        flex.setStandardHours(8);
        flex.setWorkDays("1,2,3,4,5");
        flex.setLateGraceMinutes(30);
        flex.setFlexible(true);
        flex.setStatus(1);
        flex.setCreatedAt(LocalDateTime.now());

        Shift sizeWeek = new Shift();
        sizeWeek.setName("大小周");
        sizeWeek.setType("大小周");
        sizeWeek.setWorkStart(LocalTime.of(9, 30));
        sizeWeek.setWorkEnd(LocalTime.of(18, 30));
        sizeWeek.setStandardHours(8);
        sizeWeek.setWorkDays("1,2,3,4,5,6");
        sizeWeek.setLateGraceMinutes(15);
        sizeWeek.setFlexible(false);
        sizeWeek.setStatus(1);
        sizeWeek.setCreatedAt(LocalDateTime.now());

        shiftRepository.saveAll(List.of(std, flex, sizeWeek));
    }

    // ================== 请假 ==================
    private List<LeaveRequest> seedLeaves(List<Employee> employees) {
        List<LeaveRequest> list = new ArrayList<>();
        String[] types = {"年假", "事假", "病假", "婚假", "调休", "加班"};
        String[] statuses = {"APPROVED", "PENDING", "REJECTED"};
        int pendingCount = 0;
        for (Employee e : employees) {
            if (random.nextDouble() > 0.5) continue;
            int n = random.nextInt(2) + 1;
            for (int i = 0; i < n; i++) {
                LeaveRequest l = new LeaveRequest();
                l.setEmployeeId(e.getId());
                l.setType(types[random.nextInt(types.length)]);
                LocalDate day = LocalDate.now().minusDays(random.nextInt(120));
                l.setStartTime(day.atStartOfDay().plusHours(9));
                l.setEndTime(day.atStartOfDay().plusHours(18));
                l.setDays(random.nextInt(3) + 0.5);
                l.setReason("个人事务安排");
                l.setApproverId(e.getManagerId());
                String st = statuses[random.nextInt(statuses.length)];
                if ("PENDING".equals(st)) pendingCount++;
                l.setStatus(st);
                l.setCreatedAt(day.atStartOfDay());
                l.setUpdatedAt(day.atStartOfDay());
                list.add(l);
            }
        }
        leaveRepository.saveAll(list);
        log.info("请假申请 {} 条（待审批 {}）", list.size(), pendingCount);
        return list;
    }

    // ================== 考勤 ==================
    private void seedAttendance(List<Employee> employees) {
        List<Employee> active = employees.stream()
                .filter(e -> !"LEFT".equals(e.getStatus()))
                .collect(Collectors.toList());
        List<AttendanceRecord> batch = new ArrayList<>();
        int months = 7; // 当前月 + 前6个月
        LocalDate today = LocalDate.now();
        for (int m = months - 1; m >= 0; m--) {
            YearMonth ym = YearMonth.from(today).minusMonths(m);
            // 只生成到当前月当前日
            int lastDay = ym.lengthOfMonth();
            if (m == 0) lastDay = today.getDayOfMonth();
            LocalDate first = ym.atDay(1);
            LocalDate last = ym.atDay(lastDay);
            for (Employee e : active) {
                int shiftType = e.getDepartment() != null && e.getDepartment().getCode() != null
                        && e.getDepartment().getCode().contains("SALES") ? 2 : (random.nextDouble() < 0.15 ? 1 : 0);
                LocalTime start = shiftType == 2 ? LocalTime.of(9, 30) : LocalTime.of(9, 0);
                LocalTime end = shiftType == 2 ? LocalTime.of(18, 30) : LocalTime.of(18, 0);
                // 新入职员工从入职日开始考勤
                LocalDate from = e.getHireDate().isAfter(first) ? e.getHireDate() : first;
                for (LocalDate d = from; !d.isAfter(last); d = d.plusDays(1)) {
                    int dow = d.getDayOfWeek().getValue();
                    boolean isWorkday = (shiftType == 2) ? dow != 7 : dow <= 5;
                    if (!isWorkday) continue;
                    AttendanceRecord r = new AttendanceRecord();
                    r.setEmployeeId(e.getId());
                    r.setWorkDate(d);
                    double roll = random.nextDouble();
                    r.setMethod(random.nextDouble() < 0.6 ? "WIFI" : (random.nextDouble() < 0.6 ? "定位" : "人脸"));
                    if (roll < 0.92) {
                        r.setStatus("NORMAL");
                        int lateness = random.nextInt(10);
                        r.setCheckIn(start.minusMinutes(random.nextInt(25) + 15).plusMinutes(lateness));
                        r.setCheckOut(end.plusMinutes(random.nextInt(90)));
                        r.setWorkHours(8.0 + random.nextDouble() * 1.4);
                        r.setConfirmed(true);
                    } else if (roll < 0.96) {
                        r.setStatus("LATE");
                        r.setCheckIn(start.plusMinutes(random.nextInt(75) + 15));
                        r.setCheckOut(end.plusMinutes(random.nextInt(60)));
                        r.setWorkHours(7.0 + random.nextDouble() * 1.2);
                        r.setConfirmed(false);
                        r.setRemark("上班迟到");
                    } else if (roll < 0.975) {
                        r.setStatus("EARLY");
                        r.setCheckIn(start.minusMinutes(random.nextInt(20)));
                        r.setCheckOut(end.minusMinutes(random.nextInt(90) + 30));
                        r.setWorkHours(7.0 + random.nextDouble());
                        r.setConfirmed(false);
                        r.setRemark("下班早退");
                    } else if (roll < 0.985) {
                        r.setStatus("ABSENT");
                        r.setConfirmed(false);
                        r.setRemark("未打卡缺勤");
                    } else {
                        r.setStatus("LEAVE");
                        r.setConfirmed(true);
                        r.setRemark("请假");
                        r.setWorkHours(0.0);
                    }
                    // 加班
                    if (random.nextDouble() < 0.08 && "NORMAL".equals(r.getStatus())) {
                        r.setOvertimeHours(1.0 + random.nextDouble() * 2.5);
                    }
                    r.setCreatedAt(LocalDateTime.now());
                    batch.add(r);
                }
            }
        }
        // 分批保存，避免单次过大
        for (int i = 0; i < batch.size(); i += 2000) {
            attendanceRepository.saveAll(batch.subList(i, Math.min(i + 2000, batch.size())));
        }
        log.info("考勤记录 {} 条", batch.size());
    }

    // ================== 薪酬 ==================
    private void seedPayroll(List<Employee> employees) {
        List<Employee> active = employees.stream()
                .filter(e -> !"LEFT".equals(e.getStatus()))
                .collect(Collectors.toList());
        List<PayrollRecord> batch = new ArrayList<>();
        int months = 7;
        LocalDate today = LocalDate.now();
        for (int m = months - 1; m >= 0; m--) {
            String month = YearMonth.from(today).minusMonths(m).format(MONTH_FMT);
            for (Employee e : active) {
                PayrollRecord p = new PayrollRecord();
                p.setEmployeeId(e.getId());
                p.setPayMonth(month);
                BigDecimal base = e.getBaseSalary() != null ? e.getBaseSalary() : BigDecimal.valueOf(12000);
                p.setBaseSalary(base);
                BigDecimal positionSalary = base.multiply(BigDecimal.valueOf(0.15)).setScale(0, RoundingMode.HALF_UP);
                p.setPositionSalary(positionSalary);
                // 绩效工资与绩效挂钩
                double perfFactor = 0.6 + random.nextDouble() * 0.8;
                p.setPerformanceSalary(base.multiply(BigDecimal.valueOf(0.3)).multiply(BigDecimal.valueOf(perfFactor)).setScale(0, RoundingMode.HALF_UP));
                p.setAllowance(random.nextDouble() < 0.4 ? BigDecimal.valueOf(500 + random.nextInt(800)) : BigDecimal.ZERO);
                p.setOvertimePay(random.nextDouble() < 0.3 ? BigDecimal.valueOf(200 + random.nextInt(1000)) : BigDecimal.ZERO);
                p.setBonus(random.nextDouble() < 0.15 ? base.multiply(BigDecimal.valueOf(0.2)).setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                BigDecimal gross = p.getBaseSalary().add(p.getPositionSalary()).add(p.getPerformanceSalary())
                        .add(p.getAllowance()).add(p.getOvertimePay()).add(p.getBonus());
                p.setGrossAmount(gross);
                // 五险一金（约 10.5% + 公积金 12%）
                BigDecimal social = gross.multiply(BigDecimal.valueOf(0.105)).setScale(0, RoundingMode.HALF_UP);
                p.setSocialPension(social.multiply(BigDecimal.valueOf(0.52)).setScale(0, RoundingMode.HALF_UP));
                p.setSocialMedical(social.multiply(BigDecimal.valueOf(0.33)).setScale(0, RoundingMode.HALF_UP));
                p.setSocialUnemployment(social.multiply(BigDecimal.valueOf(0.15)).setScale(0, RoundingMode.HALF_UP));
                BigDecimal fund = gross.multiply(BigDecimal.valueOf(0.12)).setScale(0, RoundingMode.HALF_UP);
                p.setHousingFund(fund);
                // 个税简化（累计预扣近似）
                BigDecimal taxable = gross.subtract(social).subtract(fund).subtract(BigDecimal.valueOf(5000));
                BigDecimal tax = taxable.compareTo(BigDecimal.ZERO) > 0
                        ? taxable.multiply(BigDecimal.valueOf(0.1)).setScale(0, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                p.setTax(tax);
                p.setDeduction(BigDecimal.ZERO);
                p.setDeductionTotal(social.add(fund).add(tax));
                p.setNetAmount(gross.subtract(social).subtract(fund).subtract(tax));
                // 状态：历史月份已发放，最近两个月核算中
                if (m == 0) p.setStatus(random.nextDouble() < 0.5 ? "CALCULATED" : "DRAFT");
                else if (m == 1) p.setStatus(random.nextDouble() < 0.8 ? "CONFIRMED" : "CALCULATED");
                else p.setStatus("PAID");
                p.setOperatorId(1L);
                p.setPayDate(LocalDate.now().minusDays(random.nextInt(3)));
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                batch.add(p);
            }
        }
        payrollRepository.saveAll(batch);
        log.info("薪酬记录 {} 条", batch.size());
    }

    // ================== 绩效 ==================
    private void seedPerformance(List<Employee> employees) {
        List<Employee> active = employees.stream()
                .filter(e -> !"LEFT".equals(e.getStatus()))
                .collect(Collectors.toList());
        List<PerformanceRecord> list = new ArrayList<>();
        String[] periods = {"2026-Q2", "2026-Q1", "2025-Q4"};
        for (String period : periods) {
            for (Employee e : active) {
                PerformanceRecord p = new PerformanceRecord();
                p.setEmployeeId(e.getId());
                p.setPeriod(period);
                p.setMode(random.nextDouble() < 0.5 ? "KPI" : (random.nextDouble() < 0.7 ? "OKR" : "360"));
                // 由九宫格反推绩效
                int cellVal = e.getGridCell() == null ? 6 : Integer.parseInt(String.valueOf(e.getGridCell().charAt(0)));
                int scoreBase = switch (cellVal) { case 9 -> 88; case 8 -> 82; case 6 -> 76; case 5 -> 70; case 3 -> 62; default -> 55; };
                double score = Math.max(40, Math.min(99, scoreBase + random.nextDouble() * 6));
                p.setScore(score);
                p.setRating(score >= 90 ? "S" : score >= 80 ? "A" : score >= 70 ? "B" : score >= 60 ? "C" : "D");
                p.setStatus("APPROVED");
                p.setInterviewNote(score >= 80 ? "目标达成度高，持续输出价值" : "存在改进空间，需加强目标对齐");
                p.setEvaluatorId(e.getManagerId());
                p.setCreatedAt(LocalDateTime.now());
                list.add(p);
            }
        }
        performanceRepository.saveAll(list);
    }

    // ================== 培训 ==================
    private void seedTraining(List<Employee> employees) {
        List<TrainingCourse> courses = new ArrayList<>();
        Object[][] cdefs = {
                {"新人入职必修课", "新人入职", "培训经理", 4.0, true},
                {"高效沟通与协作", "通用素养", "外部讲师-周老师", 6.0, true},
                {"OKR 目标管理实战", "管理能力", "总经理-王总", 8.0, true},
                {"数据可视化入门", "专业技能", "数据科学家-刘工", 10.0, true},
                {"职场压力管理", "通用素养", "心理学顾问-陈老师", 3.0, true},
                {"信息安全与合规", "合规", "法务-吴经理", 4.0, true},
                {"Java 架构设计", "专业技能", "架构师-赵工", 12.0, true},
                {"产品思维训练营", "专业技能", "产品总监-孙总", 9.0, true},
                {"领导力进阶", "管理能力", "外部商学院", 16.0, false},
                {"高效能时间管理", "通用素养", "运营-李经理", 5.0, true},
                {"商业谈判技巧", "销售技能", "销售总监-张总", 6.0, true},
                {"团队协作工作坊", "通用素养", "HR-许经理", 4.0, false},
        };
        for (Object[] c : cdefs) {
            TrainingCourse course = new TrainingCourse();
            course.setName((String) c[0]);
            course.setCategory((String) c[1]);
            course.setTeacher((String) c[2]);
            course.setHours((Double) c[3]);
            course.setOnline((Boolean) c[4]);
            course.setLearnerCount(random.nextInt(60) + 10);
            course.setAvgRating(3.5 + random.nextDouble() * 1.4);
            course.setStatus("上架");
            course.setDescription((String) c[0] + " 培训课程");
            course.setCreatedAt(LocalDateTime.now());
            courses.add(course);
        }
        courseRepository.saveAll(courses);

        List<TrainingRecord> records = new ArrayList<>();
        List<Employee> active = employees.stream().filter(e -> !"LEFT".equals(e.getStatus())).collect(Collectors.toList());
        for (Employee e : active) {
            int n = random.nextInt(3);
            for (int i = 0; i < n; i++) {
                TrainingCourse course = courses.get(random.nextInt(courses.size()));
                TrainingRecord r = new TrainingRecord();
                r.setEmployeeId(e.getId());
                r.setCourseId(course.getId());
                double done = random.nextDouble();
                if (done < 0.5) {
                    r.setStatus("已完成");
                    r.setProgress(100);
                    r.setPassed(random.nextDouble() < 0.9);
                    r.setExamScore(random.nextDouble() < 0.9 ? 60 + random.nextDouble() * 40 : 40 + random.nextDouble() * 20);
                    r.setCompletedAt(LocalDateTime.now().minusDays(random.nextInt(60)));
                } else if (done < 0.8) {
                    r.setStatus("进行中");
                    r.setProgress(random.nextInt(80) + 20);
                    r.setPassed(null);
                } else {
                    r.setStatus("未开始");
                    r.setProgress(0);
                    r.setPassed(null);
                }
                r.setEnrolledAt(LocalDateTime.now().minusDays(random.nextInt(90)));
                records.add(r);
            }
        }
        trainingRecordRepository.saveAll(records);
        log.info("培训课程 {} 门，学习记录 {} 条", courses.size(), records.size());
    }

    // ================== 招聘 ==================
    private void seedRecruitment(Map<Long, Department> deptMap) {
        List<JobRequisition> jobs = new ArrayList<>();
        Object[][] jdefs = {
                {"高级 Java 工程师", 3L, 5, "高"},
                {"前端工程师", 4L, 3, "中"},
                {"产品经理", 9L, 2, "高"},
                {"数据分析师", 7L, 2, "中"},
                {"大客户经理", 16L, 4, "高"},
                {"销售代表", 17L, 6, "中"},
                {"UI设计师", 10L, 2, "低"},
                {"HRBP", 19L, 2, "中"},
        };
        for (Object[] j : jdefs) {
            JobRequisition rq = new JobRequisition();
            rq.setTitle((String) j[0]);
            rq.setDeptId((Long) j[1]);
            rq.setHeadcount((Integer) j[2]);
            rq.setFilled(random.nextInt((Integer) j[2]));
            rq.setStatus(rq.getFilled() >= rq.getHeadcount() ? "FILLED" : "OPEN");
            rq.setPriority((String) j[3]);
            rq.setCreatedAt(LocalDateTime.now());
            jobs.add(rq);
        }
        jobRepository.saveAll(jobs);

        List<Candidate> candidates = new ArrayList<>();
        String[] channels = {"BOSS直聘", "拉勾", "内推", "猎聘", "校园招聘", "官网"};
        String[] posPool = {"高级Java工程师", "前端工程师", "产品经理", "数据分析师", "大客户经理", "销售代表", "UI设计师", "HRBP", "测试工程师", "运维工程师"};
        String[] statusPool = {"简历筛选", "初试", "复试", "HR面", "Offer", "入职", "淘汰"};
        for (int i = 0; i < 90; i++) {
            Candidate c = new Candidate();
            String gender = random.nextDouble() < 0.5 ? "MALE" : "FEMALE";
            c.setName(genName(gender));
            c.setPhone("137" + (10000000 + random.nextInt(89999999)));
            c.setEmail("candidate" + (i + 1) + "@mail.com");
            c.setPosition(posPool[random.nextInt(posPool.length)]);
            c.setChannel(channels[random.nextInt(channels.length)]);
            c.setEducation(random.nextDouble() < 0.1 ? "博士" : (random.nextDouble() < 0.45 ? "硕士" : "本科"));
            c.setWorkYears(random.nextInt(10) + 1);
            c.setResumeScore(random.nextInt(50) + 45);
            c.setStatus(statusPool[random.nextInt(statusPool.length)]);
            c.setApplyDate(LocalDate.now().minusDays(random.nextInt(60)));
            c.setRemark("候选人来自" + c.getChannel());
            c.setCreatedAt(LocalDateTime.now());
            candidates.add(c);
        }
        candidateRepository.saveAll(candidates);
        log.info("候选人 {} 名，招聘需求 {} 条", candidates.size(), jobs.size());
    }

    // ================== 公告 ==================
    private void seedAnnouncements() {
        Object[][] defs = {
                {"2026年度年中全员大会通知", "公告", "定于本月25日召开全员大会，请各部门提前安排好工作。", true},
                {"新版《考勤管理制度》发布", "制度", "新版考勤管理制度自下月起执行，弹性班次调整详见制度全文。", true},
                {"第三季度团建活动报名", "活动", "三季度团建活动报名通道已开启，截止日期为本月20日。", false},
                {"全员健康体检通知", "通知", "年度健康体检将于下月启动，请各部门关注邮件通知安排。", false},
                {"IT 办公系统升级维护", "通知", "本周末办公系统将升级维护，届时考勤打卡将切换到应急模式。", false},
                {"2026上半年优秀员工表彰", "公告", "经各部门推荐与评审，本年度上半年共评选出优秀员工 12 名。", false},
                {"培训课程上新", "活动", "《数据可视化入门》《高效能时间管理》等课程已上线学习平台。", false},
        };
        List<Announcement> list = new ArrayList<>();
        for (Object[] a : defs) {
            Announcement an = new Announcement();
            an.setTitle((String) a[0]);
            an.setType((String) a[1]);
            an.setContent((String) a[2]);
            an.setPublisherId(1L);
            an.setStatus("PUBLISHED");
            an.setPinned((Boolean) a[3]);
            an.setPublishTime(LocalDateTime.now().minusDays(random.nextInt(30)));
            an.setCreatedAt(LocalDateTime.now());
            list.add(an);
        }
        announcementRepository.saveAll(list);
    }

    // ================== 待办 ==================
    private void seedTodos(List<Employee> employees) {
        List<Todo> list = new ArrayList<>();
        // HR 待办
        String[][] hrTodos = {
                {"3名新员工入职手续待办理", "入职", "高"},
                {"2份劳动合同即将到期提醒", "合同到期", "中"},
                {"8月员工转正评审待发起", "转正", "高"},
                {"薪酬核算流程待确认", "审批", "高"},
                {"季度人才盘点资料待整理", "审批", "中"},
        };
        for (String[] t : hrTodos) {
            list.add(buildTodo(t[0], t[1], 2L, t[2]));
        }
        // 管理端待办
        String[][] mgrTodos = {
                {"3条请假申请待审批", "审批", "高"},
                {"5条考勤异常待确认", "考勤异常", "中"},
                {"下属季度绩效待评分", "审批", "高"},
        };
        for (String[] t : mgrTodos) {
            list.add(buildTodo(t[0], t[1], 3L, t[2]));
            list.add(buildTodo(t[0], t[1], 5L, t[2]));
        }
        // 员工待办：请假审批
        int empCount = 0;
        for (Employee e : employees) {
            if (empCount > 0) break;
            if ("ACTIVE".equals(e.getStatus())) {
                list.add(buildTodo("您的年假申请正在审批中", "审批", 4L, "中"));
                empCount++;
            }
        }
        todoRepository.saveAll(list);
    }

    private Todo buildTodo(String title, String type, Long assigneeId, String priority) {
        Todo t = new Todo();
        t.setTitle(title);
        t.setType(type);
        t.setAssigneeId(assigneeId);
        t.setPriority(priority);
        t.setStatus("PENDING");
        t.setDueTime(LocalDateTime.now().plusDays(2));
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    // ================== 证明 ==================
    private void seedCertificates(List<Employee> employees) {
        List<Certificate> list = new ArrayList<>();
        int count = 0;
        for (Employee e : employees) {
            if ("LEFT".equals(e.getStatus()) || random.nextDouble() > 0.15) continue;
            if (count > 20) break;
            Certificate c = new Certificate();
            c.setEmployeeId(e.getId());
            c.setType(random.nextDouble() < 0.6 ? "EMPLOYMENT" : "INCOME");
            c.setPurpose(random.nextDouble() < 0.5 ? "银行办理" : "公积金提取");
            c.setStatus(random.nextDouble() < 0.7 ? "ISSUED" : "PENDING");
            c.setEffectiveDate(LocalDate.now());
            c.setCreatedAt(LocalDateTime.now());
            list.add(c);
            count++;
        }
        certificateRepository.saveAll(list);
    }

    // ================== 操作日志 ==================
    private void seedOperationLogs() {
        String[][] logs = {
                {"员工档案", "CREATE", "新增员工档案：陈晓东"},
                {"员工档案", "UPDATE", "修改员工档案：王丽婷 岗位调整"},
                {"考勤", "EXPORT", "导出 2026-07 月考勤汇总表"},
                {"薪酬", "UPDATE", "完成 2026-07 月薪酬核算"},
                {"招聘", "CREATE", "新增候选人：候选人32 应聘高级Java工程师"},
                {"绩效", "EXPORT", "导出 2026-Q2 绩效分布报表"},
                {"系统", "LOGIN", "用户 admin 登录系统"},
                {"组织", "UPDATE", "调整后端研发部组织架构"},
        };
        List<OperationLog> list = new ArrayList<>();
        for (String[] l : logs) {
            OperationLog o = new OperationLog();
            o.setOperatorId(1L);
            o.setOperatorName("系统管理员");
            o.setModule(l[0]);
            o.setAction(l[1]);
            o.setDescription(l[2]);
            o.setIp("192.168.1." + (random.nextInt(200) + 1));
            o.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(15)));
            list.add(o);
        }
        operationLogRepository.saveAll(list);
    }

    // ================== 工具方法 ==================
    private String genName(String gender) {
        String surname = SURNAMES[random.nextInt(SURNAMES.length)];
        if (random.nextDouble() < 0.25) {
            // 双字名
            String[] givenPool = "MALE".equals(gender) ? GIVEN_MALE : GIVEN_FEMALE;
            return surname + givenPool[random.nextInt(givenPool.length)] + givenPool[random.nextInt(givenPool.length)];
        }
        String[] givenPool = "MALE".equals(gender) ? GIVEN_MALE : GIVEN_FEMALE;
        return surname + givenPool[random.nextInt(givenPool.length)];
    }

    private String pinyinOrFallback(String name) {
        String[] arr = {"chen", "wang", "li", "zhang", "liu", "yang", "zhou", "zhao", "huang", "sun",
                "xu", "gao", "lin", "he", "guo", "ma", "luo", "liang", "song", "zheng"};
        return arr[random.nextInt(arr.length)] + (random.nextInt(90) + 10);
    }

    private String randomAddress() {
        String[] streets = {"朝阳区望京街道", "海淀区中关村大街", "西城区金融街", "丰台区科技园区",
                "浦东新区张江高科技园", "南山区科技园", "天河区珠江新城", "西湖区文三路"};
        return streets[random.nextInt(streets.length)] + random.nextInt(200) + "号";
    }
}
