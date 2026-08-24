package com.hrms.service;

import com.hrms.domain.attendance.AttendanceRecord;
import com.hrms.domain.attendance.LeaveRequest;
import com.hrms.domain.employee.Employee;
import com.hrms.domain.hr.Announcement;
import com.hrms.domain.hr.Todo;
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

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final LeaveRequestRepository leaveRepository;
    private final TodoRepository todoRepository;
    private final AnnouncementRepository announcementRepository;
    private final PayrollRecordRepository payrollRepository;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        List<Employee> all = employeeRepository.findAll();
        long active = all.stream().filter(e -> "ACTIVE".equals(e.getStatus()) || "PROBATION".equals(e.getStatus())).count();
        long probation = all.stream().filter(e -> "PROBATION".equals(e.getStatus())).count();
        YearMonth ym = YearMonth.now();
        long newHires = all.stream().filter(e -> e.getHireDate() != null
                && YearMonth.from(e.getHireDate()).equals(ym)).count();
        long resignations = all.stream().filter(e -> e.getLeaveDate() != null
                && YearMonth.from(e.getLeaveDate()).equals(ym)).count();
        double attritionRate = active > 0 ? resignations * 100.0 / active : 0;
        double avgSalary = all.stream()
                .filter(e -> "ACTIVE".equals(e.getStatus()))
                .filter(e -> e.getBaseSalary() != null)
                .mapToDouble(e -> e.getBaseSalary().doubleValue())
                .average().orElse(0);

        // 本月考勤率
        LocalDate first = ym.atDay(1);
        LocalDate last = ym.atEndOfMonth();
        if (last.isAfter(LocalDate.now())) last = LocalDate.now();
        List<AttendanceRecord> monthRecords = attendanceRepository.findByWorkDateBetweenOrderByWorkDateAsc(first, last);
        long totalDays = monthRecords.size();
        long normalDays = monthRecords.stream().filter(r -> "NORMAL".equals(r.getStatus())).count();
        double attendanceRate = totalDays > 0 ? normalDays * 100.0 / totalDays : 100;

        // 待办
        long pendingLeave = leaveRepository.findByStatusOrderByCreatedAtDesc("PENDING").size();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalEmployees", active);
        result.put("probation", probation);
        result.put("newHires", newHires);
        result.put("resignations", resignations);
        result.put("attritionRate", round1(attritionRate));
        result.put("avgSalary", Math.round(avgSalary));
        result.put("attendanceRate", round1(attendanceRate));
        result.put("pendingLeave", pendingLeave);
        result.put("payrollMonth", YearMonth.now().format(MONTH_FMT));
        return result;
    }

    /** 近12个月人员趋势（在职/入职/离职） */
    @Transactional(readOnly = true)
    public Map<String, Object> headcountTrend() {
        List<Employee> all = employeeRepository.findAll();
        List<String> months = new ArrayList<>();
        List<Long> headcounts = new ArrayList<>();
        List<Long> hires = new ArrayList<>();
        List<Long> leaves = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            months.add(ym.format(MONTH_FMT));
            LocalDate monthEnd = ym.atEndOfMonth();
            LocalDate monthStart = ym.atDay(1);
            long hc = all.stream().filter(e ->
                    (e.getHireDate() != null && !e.getHireDate().isAfter(monthEnd))
                            && (e.getLeaveDate() == null || e.getLeaveDate().isAfter(monthEnd))).count();
            long h = all.stream().filter(e -> e.getHireDate() != null
                    && !e.getHireDate().isBefore(monthStart) && !e.getHireDate().isAfter(monthEnd)).count();
            long l = all.stream().filter(e -> e.getLeaveDate() != null
                    && !e.getLeaveDate().isBefore(monthStart) && !e.getLeaveDate().isAfter(monthEnd)).count();
            headcounts.add(hc);
            hires.add(h);
            leaves.add(l);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", months);
        result.put("headcounts", headcounts);
        result.put("hires", hires);
        result.put("leaves", leaves);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> myTodos(Long userId) {
        List<Todo> todos = todoRepository.findByAssigneeIdAndStatusOrderByPriorityAscCreatedAtDesc(userId, "PENDING");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Todo t : todos) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("type", t.getType());
            m.put("priority", t.getPriority());
            m.put("dueTime", t.getDueTime());
            result.add(m);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Announcement> announcements() {
        return announcementRepository.findTop10ByStatusOrderByPublishTimeDesc("PUBLISHED");
    }

    /** 薪酬成本近6个月趋势 */
    @Transactional(readOnly = true)
    public Map<String, Object> payrollTrend() {
        YearMonth now = YearMonth.now();
        List<String> months = new ArrayList<>();
        List<BigDecimal> costs = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            String month = now.minusMonths(i).format(MONTH_FMT);
            months.add(month);
            BigDecimal total = payrollRepository.findByPayMonth(month).stream()
                    .map(r -> r.getGrossAmount() == null ? BigDecimal.ZERO : r.getGrossAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            costs.add(total);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", months);
        result.put("costs", costs);
        return result;
    }

    private double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
