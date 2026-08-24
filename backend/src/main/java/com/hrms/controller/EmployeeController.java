package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.common.PageResult;
import com.hrms.domain.employee.Employee;
import com.hrms.dto.EmployeeVO;
import com.hrms.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiResponse<PageResult<EmployeeVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(employeeService.search(keyword, deptId, status, level, education, gender, tag, page, size));
    }

    @GetMapping("/tags")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiResponse<List<String>> tags() {
        return ApiResponse.ok(employeeService.allTags());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(employeeService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiResponse<Employee> create(@RequestBody Employee employee) {
        return ApiResponse.ok(employeeService.create(employee));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiResponse<Employee> update(@PathVariable Long id, @RequestBody Employee employee) {
        return ApiResponse.ok(employeeService.update(id, employee));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ApiResponse.ok();
    }
}
