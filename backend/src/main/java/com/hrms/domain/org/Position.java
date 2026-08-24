package com.hrms.domain.org;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 职位（岗位，企业级统一）
 */
@Data
@Entity
@Table(name = "org_position")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 32)
    private String code;

    /** 职级体系：P1-P8（专业序列）、M1-M5（管理序列） */
    @Column(length = 16)
    private String level;

    /** 所属部门（可空，表示通用岗位） */
    private Long deptId;

    /** 岗位序列：管理/技术/产品/运营/市场/职能 */
    @Column(length = 32)
    private String category;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
