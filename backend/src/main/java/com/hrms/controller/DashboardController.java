package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.common.SecurityUtil;
import com.hrms.domain.hr.Announcement;
import com.hrms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.ok(dashboardService.summary());
    }

    @GetMapping("/headcount-trend")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiResponse<Map<String, Object>> headcountTrend() {
        return ApiResponse.ok(dashboardService.headcountTrend());
    }

    @GetMapping("/payroll-trend")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiResponse<Map<String, Object>> payrollTrend() {
        return ApiResponse.ok(dashboardService.payrollTrend());
    }

    @GetMapping("/todos")
    public ApiResponse<List<Map<String, Object>>> todos() {
        return ApiResponse.ok(dashboardService.myTodos(SecurityUtil.currentUser().id()));
    }

    @GetMapping("/announcements")
    public ApiResponse<List<Announcement>> announcements() {
        return ApiResponse.ok(dashboardService.announcements());
    }
}
