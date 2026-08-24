package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','HR')")
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(@RequestParam String month) {
        return ApiResponse.ok(payrollService.list(month));
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@RequestParam String month) {
        return ApiResponse.ok(payrollService.summary(month));
    }

    @GetMapping("/dept-cost")
    public ApiResponse<Map<String, Object>> deptCost(@RequestParam String month) {
        return ApiResponse.ok(payrollService.deptCost(month));
    }

    @GetMapping("/payslip")
    public ApiResponse<Map<String, Object>> payslip(@RequestParam Long employeeId,
                                                    @RequestParam String month) {
        return ApiResponse.ok(payrollService.payslip(employeeId, month));
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiResponse<Integer> calculate(@RequestParam String month) {
        return ApiResponse.ok(payrollService.calculate(month));
    }

    /** 确认 / 发放：持久化单条薪酬状态（此前仅前端本地改动，刷新即丢失） */
    @PutMapping("/records/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        payrollService.updateStatus(id, status);
        return ApiResponse.ok();
    }
}
