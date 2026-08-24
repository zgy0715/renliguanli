package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.domain.attendance.Shift;
import com.hrms.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
public class AttendanceController {

    private final AttendanceService attendanceService;

    /** 月历视图 */
    @GetMapping("/calendar")
    public ApiResponse<Map<String, Object>> calendar(@RequestParam String month) {
        return ApiResponse.ok(attendanceService.calendar(month));
    }

    /** 月度汇总（每人一行） */
    @GetMapping("/monthly")
    public ApiResponse<Map<String, Object>> monthly(@RequestParam String month,
                                                    @RequestParam(required = false) Long deptId) {
        return ApiResponse.ok(attendanceService.monthly(month, deptId));
    }

    /** 异常明细 */
    @GetMapping("/exceptions")
    public ApiResponse<List<Map<String, Object>>> exceptions(@RequestParam String month) {
        return ApiResponse.ok(attendanceService.exceptions(month));
    }

    @GetMapping("/shifts")
    public ApiResponse<List<Shift>> shifts() {
        return ApiResponse.ok(attendanceService.shifts());
    }

    @PutMapping("/records/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiResponse<Void> confirm(@PathVariable Long id, @RequestParam Boolean confirmed) {
        attendanceService.confirmException(id, confirmed);
        return ApiResponse.ok();
    }
}
