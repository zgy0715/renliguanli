package com.hrms.service;

import com.hrms.domain.attendance.AttendanceRecord;
import com.hrms.domain.attendance.Shift;
import com.hrms.domain.employee.Employee;
import com.hrms.repository.AttendanceRecordRepository;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.ShiftRepository;
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
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /** 月历视图：每天的出勤/迟到/早退/缺勤/请假/外勤人数 */
    @Transactional(readOnly = true)
    public Map<String, Object> calendar(String month) {
        YearMonth ym = YearMonth.parse(month, MONTH_FMT);
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();
        if (last.isAfter(LocalDate.now())) last = LocalDate.now();
        List<AttendanceRecord> records = attendanceRepository.findByWorkDateBetweenOrderByWorkDateAsc(first, last);

        Map<LocalDate, Map<String, Object>> dayMap = new LinkedHashMap<>();
        List<String> days = new ArrayList<>();
        List<Map<String, Object>> daysData = new ArrayList<>();
        long totalLate = 0, totalAbsent = 0, totalEarly = 0, totalNormal = 0;
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            Map<String, Object> cell = new LinkedHashMap<>();
            cell.put("date", d.toString());
            cell.put("weekday", d.getDayOfWeek().getValue());
            cell.put("isWorkday", d.getDayOfWeek().getValue() <= 5);
            long normal = 0, late = 0, early = 0, absent = 0, leave = 0, field = 0;
            for (AttendanceRecord r : records) {
                if (r.getWorkDate().equals(d)) {
                    switch (r.getStatus() == null ? "" : r.getStatus()) {
                        case "NORMAL" -> normal++;
                        case "LATE" -> late++;
                        case "EARLY" -> early++;
                        case "ABSENT" -> absent++;
                        case "LEAVE" -> leave++;
                        case "FIELD" -> field++;
                        default -> {
                        }
                    }
                }
            }
            cell.put("normal", normal);
            cell.put("late", late);
            cell.put("early", early);
            cell.put("absent", absent);
            cell.put("leave", leave);
            cell.put("field", field);
            long total = normal + late + early + absent + leave + field;
            cell.put("total", total);
            // 无记录日（周末/无数据）rate 置 0，避免被渲染成最深的“出勤满”热力色
            cell.put("rate", total > 0 ? round1(normal * 100.0 / total) : 0);
            dayMap.put(d, cell);
            days.add(d.toString());
            daysData.add(cell);
            totalLate += late;
            totalAbsent += absent;
            totalEarly += early;
            totalNormal += normal;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month);
        result.put("days", daysData);
        result.put("stat", Map.of(
                "normal", totalNormal, "late", totalLate, "early", totalEarly, "absent", totalAbsent,
                "attendanceRate", round1((totalNormal + totalLate + totalEarly) * 100.0 / Math.max(1, totalNormal + totalLate + totalEarly + totalAbsent))));
        return result;
    }

    /** 月度汇总：每个在职员工的出勤统计 */
    @Transactional(readOnly = true)
    public Map<String, Object> monthly(String month, Long deptId) {
        YearMonth ym = YearMonth.parse(month, MONTH_FMT);
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();
        if (last.isAfter(LocalDate.now())) last = LocalDate.now();

        List<Employee> employees = employeeRepository.findByStatusNotOrderByHireDateAsc("LEFT").stream()
                .filter(e -> deptId == null || (e.getDepartment() != null && e.getDepartment().getId().equals(deptId)))
                .collect(Collectors.toList());
        List<AttendanceRecord> records = attendanceRepository.findByWorkDateBetweenOrderByWorkDateAsc(first, last);
        Map<Long, List<AttendanceRecord>> byEmp = records.stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getEmployeeId));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Employee e : employees) {
            List<AttendanceRecord> list = byEmp.getOrDefault(e.getId(), Collections.emptyList());
            long normal = count(list, "NORMAL");
            long late = count(list, "LATE");
            long early = count(list, "EARLY");
            long absent = count(list, "ABSENT");
            long leave = count(list, "LEAVE");
            long field = count(list, "FIELD");
            double overtime = list.stream().mapToDouble(r -> r.getOvertimeHours() == null ? 0 : r.getOvertimeHours()).sum();
            long total = list.size();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", e.getId());
            row.put("employeeNo", e.getEmployeeNo());
            row.put("name", e.getName());
            row.put("department", e.getDepartment() != null ? e.getDepartment().getName() : "-");
            row.put("totalDays", total);
            row.put("normal", normal);
            row.put("late", late);
            row.put("early", early);
            row.put("absent", absent);
            row.put("leave", leave);
            row.put("field", field);
            row.put("overtime", round1(overtime));
            row.put("rate", total > 0 ? round1((normal + late + early + field) * 100.0 / total) : 0);
            row.put("hasException", late + early + absent > 0);
            rows.add(row);
        }
        // 按异常优先排序（异常行置顶，便于 HR 优先处理）
        rows.sort(Comparator.comparing(r -> !Boolean.TRUE.equals(r.get("hasException"))));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month);
        result.put("rows", rows);
        return result;
    }

    /** 异常明细 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> exceptions(String month) {
        YearMonth ym = YearMonth.parse(month, MONTH_FMT);
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();
        if (last.isAfter(LocalDate.now())) last = LocalDate.now();
        Map<Long, Employee> empMap = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        List<Map<String, Object>> list = new ArrayList<>();
        for (AttendanceRecord r : attendanceRepository.findByWorkDateBetweenOrderByWorkDateAsc(first, last)) {
            String st = r.getStatus();
            if (!"LATE".equals(st) && !"EARLY".equals(st) && !"ABSENT".equals(st)) continue;
            Employee e = empMap.get(r.getEmployeeId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("employeeId", r.getEmployeeId());
            m.put("name", e != null ? e.getName() : "-");
            m.put("employeeNo", e != null ? e.getEmployeeNo() : "-");
            m.put("department", e != null && e.getDepartment() != null ? e.getDepartment().getName() : "-");
            m.put("date", r.getWorkDate().toString());
            m.put("status", st);
            m.put("checkIn", r.getCheckIn() != null ? r.getCheckIn().toString() : "-");
            m.put("checkOut", r.getCheckOut() != null ? r.getCheckOut().toString() : "-");
            m.put("remark", r.getRemark());
            m.put("confirmed", r.getConfirmed());
            list.add(m);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<Shift> shifts() {
        return shiftRepository.findByStatusOrderByIdAsc(1);
    }

    @Transactional
    public void confirmException(Long recordId, Boolean confirmed) {
        AttendanceRecord r = attendanceRepository.findById(recordId)
                .orElseThrow(() -> new com.hrms.common.BizException("记录不存在"));
        r.setConfirmed(confirmed);
        attendanceRepository.save(r);
    }

    private long count(List<AttendanceRecord> list, String status) {
        return list.stream().filter(r -> status.equals(r.getStatus())).count();
    }

    private double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
