package com.hrms.dto;

/**
 * 当前用户信息
 */
public record UserInfo(
        Long id,
        String username,
        String displayName,
        String role,
        Long employeeId,
        Long avatarColor) {
}
