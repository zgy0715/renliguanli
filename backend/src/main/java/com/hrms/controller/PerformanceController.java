package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.domain.performance.PerformanceRecord;
import com.hrms.repository.EmployeeRepository;
import com.hrms.repository.PerformanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceRecordRepository performanceRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(required = false) String period) {
        List<PerformanceRecord> records = (period == null || period.isBlank())
                ? performanceRepository.findAll()
                : performanceRepository.findByPeriod(period);
        Map<Long, String> empNames = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getId(), e -> e.getName(), (a, b) -> a));
        List<Map<String, Object>> rows = records.stream()
                // 按期间倒序、得分倒序（类型化比较，避免字符串拼接 "2026-Q2" + score 导致 5.0 > 50.0 的错序）
                .sorted(Comparator.comparing((PerformanceRecord r) -> r.getPeriod() == null ? "" : r.getPeriod())
                        .thenComparing(r -> r.getScore() == null ? 0 : r.getScore()).reversed())
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("employeeId", r.getEmployeeId());
                    m.put("name", empNames.getOrDefault(r.getEmployeeId(), "-"));
                    m.put("period", r.getPeriod());
                    m.put("mode", r.getMode());
                    m.put("score", r.getScore());
                    m.put("rating", r.getRating());
                    m.put("status", r.getStatus());
                    m.put("interviewNote", r.getInterviewNote());
                    return m;
                }).collect(Collectors.toList());
        return ApiResponse.ok(rows);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiResponse<PerformanceRecord> create(@RequestBody PerformanceRecord record) {
        record.setCreatedAt(LocalDateTime.now());
        return ApiResponse.ok(performanceRepository.save(record));
    }
}
