package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 数据驾驶舱 / 专项分析 API
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/structure")
    public ApiResponse<Map<String, Object>> structure(@RequestParam(defaultValue = "age") String dimension) {
        return ApiResponse.ok(analyticsService.structure(dimension));
    }

    @GetMapping("/dept-headcount")
    public ApiResponse<Map<String, Object>> deptHeadcount() {
        return ApiResponse.ok(analyticsService.deptHeadcount());
    }

    @GetMapping("/attrition-trend")
    public ApiResponse<Map<String, Object>> attritionTrend() {
        return ApiResponse.ok(analyticsService.attritionTrend());
    }

    @GetMapping("/nine-box")
    public ApiResponse<Map<String, Object>> nineBox() {
        return ApiResponse.ok(analyticsService.nineBox());
    }

    @GetMapping("/recruitment-funnel")
    public ApiResponse<Map<String, Object>> recruitmentFunnel() {
        return ApiResponse.ok(analyticsService.recruitmentFunnel());
    }

    @GetMapping("/recruitment-channel")
    public ApiResponse<Map<String, Object>> recruitmentChannel() {
        return ApiResponse.ok(analyticsService.recruitmentChannel());
    }

    @GetMapping("/attendance-trend")
    public ApiResponse<Map<String, Object>> attendanceTrend() {
        return ApiResponse.ok(analyticsService.attendanceTrend());
    }

    @GetMapping("/performance-distribution")
    public ApiResponse<Map<String, Object>> performanceDistribution(@RequestParam(defaultValue = "2026-Q2") String period) {
        return ApiResponse.ok(analyticsService.performanceDistribution(period));
    }

    @GetMapping("/dept-efficiency")
    public ApiResponse<Map<String, Object>> deptEfficiency() {
        return ApiResponse.ok(analyticsService.deptEfficiency());
    }

    @GetMapping("/training-stats")
    public ApiResponse<Map<String, Object>> trainingStats() {
        return ApiResponse.ok(analyticsService.trainingStats());
    }

    @GetMapping("/payroll-cost-trend")
    public ApiResponse<Map<String, Object>> payrollCostTrend() {
        return ApiResponse.ok(analyticsService.payrollCostTrend());
    }

    @GetMapping("/capability-radar")
    public ApiResponse<Map<String, Object>> capabilityRadar(@RequestParam(required = false, defaultValue = "技术中心") String dept) {
        return ApiResponse.ok(analyticsService.capabilityRadar(dept));
    }
}
