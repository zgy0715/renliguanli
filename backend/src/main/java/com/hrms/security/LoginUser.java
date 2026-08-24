package com.hrms.security;

/**
 * 当前登录用户上下文信息
 */
public record LoginUser(Long id, String username, String displayName, String role, Long employeeId) {
}
