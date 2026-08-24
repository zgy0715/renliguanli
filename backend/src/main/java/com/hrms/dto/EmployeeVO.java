package com.hrms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工列表行视图（敏感字段已脱敏）
 */
public record EmployeeVO(
        Long id,
        String employeeNo,
        String name,
        String gender,
        LocalDate birthDate,
        String phone,
        String email,
        Long departmentId,
        String departmentName,
        Long positionId,
        String positionName,
        String level,
        LocalDate hireDate,
        LocalDate regularDate,
        String status,
        String education,
        BigDecimal baseSalary,
        String tags,
        String gridCell,
        Long managerId,
        String managerName,
        Integer profileScore,
        String avatar) {
}
