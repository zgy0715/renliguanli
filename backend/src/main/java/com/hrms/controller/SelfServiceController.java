package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.common.BizException;
import com.hrms.common.SecurityUtil;
import com.hrms.domain.attendance.AttendanceRecord;
import com.hrms.domain.employee.Employee;
import com.hrms.domain.hr.Certificate;
import com.hrms.domain.payroll.PayrollRecord;
import com.hrms.domain.performance.PerformanceRecord;
import com.hrms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工自助服务
 */
@RestController
@RequestMapping("/api/self-service")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYEE')")
public class SelfServiceController {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final PayrollRecordRepository payrollRepository;
    private final CertificateRepository certificateRepository;
    private final LeaveRequestRepository leaveRepository;
    private final PerformanceRecordRepository performanceRepository;

    /** 我的档案 */
    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        Employee e = currentEmployee();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("employeeNo", e.getEmployeeNo());
        m.put("name", e.getName());
        m.put("gender", e.getGender());
        m.put("birthDate", e.getBirthDate());
        m.put("phone", e.getPhone());
        m.put("email", e.getEmail());
        m.put("department", e.getDepartment() != null ? e.getDepartment().getName() : "-");
        m.put("position", e.getPosition() != null ? e.getPosition().getName() : "-");
        m.put("level", e.getLevel());
        m.put("hireDate", e.getHireDate());
        m.put("regularDate", e.getRegularDate());
        m.put("education", e.getEducation());
        m.put("school", e.getSchool());
        m.put("major", e.getMajor());
        m.put("status", e.getStatus());
        m.put("tags", e.getTags());
        m.put("gridCell", e.getGridCell());
        return ApiResponse.ok(m);
    }

    /** 假期余额（简化计算） */
    @GetMapping("/leave-balance")
    public ApiResponse<Map<String, Object>> leaveBalance() {
        Employee e = currentEmployee();
        int years = e.getHireDate() == null ? 0 : (int) java.time.temporal.ChronoUnit.YEARS.between(e.getHireDate(), LocalDate.now());
        double annual = years >= 10 ? 15 : years >= 5 ? 10 : 5;
        // 仅「年假」类型的已批记录计入年假已用（事假/病假/调休等不挤占年假额度）
        double used = leaveRepository.findByEmployeeIdOrderByCreatedAtDesc(e.getId()).stream()
                .filter(l -> "APPROVED".equals(l.getStatus()) && "年假".equals(l.getType()))
                .mapToDouble(l -> l.getDays() == null ? 0 : l.getDays())
                .sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("annualLeaveTotal", annual);
        result.put("annualLeaveUsed", Math.min(annual, used));
        result.put("annualLeaveRemain", Math.max(0, annual - used));
        result.put("sickLeave", 12);
        result.put("personalLeave", 10);
        result.put("compTime", 16.0);
        return ApiResponse.ok(result);
    }

    /** 我的工资条 */
    @GetMapping("/payslips")
    public ApiResponse<List<Map<String, Object>>> payslips() {
        Employee e = currentEmployee();
        // 一次查询该员工全部工资条（此前 findAll()+逐月查存在 N+1 且物化全公司薪资）
        List<Map<String, Object>> list = new ArrayList<>();
        for (PayrollRecord r : payrollRepository.findByEmployeeIdOrderByPayMonthDesc(e.getId())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", r.getPayMonth());
            m.put("gross", r.getGrossAmount());
            m.put("net", r.getNetAmount());
            m.put("status", r.getStatus());
            m.put("payDate", r.getPayDate());
            list.add(m);
        }
        return ApiResponse.ok(list);
    }

    /** 我的证明 */
    @GetMapping("/certificates")
    public ApiResponse<List<Certificate>> certificates() {
        return ApiResponse.ok(certificateRepository.findByEmployeeIdOrderByCreatedAtDesc(currentEmployee().getId()));
    }

    @PostMapping("/certificates")
    public ApiResponse<Certificate> applyCertificate(@RequestParam String type, @RequestParam String purpose) {
        Certificate c = new Certificate();
        c.setEmployeeId(currentEmployee().getId());
        c.setType(type);
        c.setPurpose(purpose);
        c.setStatus("PENDING");
        c.setEffectiveDate(LocalDate.now());
        c.setCreatedAt(LocalDateTime.now());
        return ApiResponse.ok(certificateRepository.save(c));
    }

    /** 我的出勤 */
    @GetMapping("/attendance")
    public ApiResponse<List<AttendanceRecord>> myAttendance(@RequestParam(required = false) String month) {
        Employee e = currentEmployee();
        YearMonth ym = (month == null || month.isBlank()) ? YearMonth.now() : YearMonth.parse(month);
        return ApiResponse.ok(attendanceRepository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
                e.getId(), ym.atDay(1), ym.atEndOfMonth()));
    }

    /** 我的绩效 */
    @GetMapping("/performance")
    public ApiResponse<List<PerformanceRecord>> myPerformance() {
        Employee e = currentEmployee();
        return ApiResponse.ok(performanceRepository.findByEmployeeIdOrderByPeriodDesc(e.getId()));
    }

    private Employee currentEmployee() {
        Long employeeId = SecurityUtil.currentUser().employeeId();
        if (employeeId == null) {
            throw new BizException(403, "当前账号未绑定员工档案");
        }
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BizException("员工档案不存在"));
    }
}
