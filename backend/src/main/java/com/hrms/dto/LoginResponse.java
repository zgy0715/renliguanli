package com.hrms.dto;

/**
 * 登录响应
 */
public record LoginResponse(
        String token,
        Long userId,
        String username,
        String displayName,
        String role,
        Long employeeId,
        long expire) {
}
