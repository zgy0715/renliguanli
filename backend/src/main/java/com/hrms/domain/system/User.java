package com.hrms.domain.system;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户（登录账号）
 */
@Data
@Entity
@Table(name = "sys_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录名 */
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /** 密码（BCrypt） */
    @Column(nullable = false)
    private String password;

    /** 显示名称 */
    @Column(length = 64)
    private String displayName;

    /** 角色：ADMIN / HR / MANAGER / EMPLOYEE */
    @Column(length = 32, nullable = false)
    private String role;

    /** 关联员工ID（员工自助账号） */
    private Long employeeId;

    /** 状态：1启用 0禁用 */
    private Integer status = 1;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
