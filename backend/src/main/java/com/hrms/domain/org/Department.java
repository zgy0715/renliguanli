package com.hrms.domain.org;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门（组织架构树节点）
 */
@Data
@Entity
@Table(name = "org_department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 32)
    private String code;

    /** 上级部门ID，根节点为 null */
    private Long parentId;

    /** 部门负责人（员工ID） */
    private Long leaderId;

    private Integer sort = 0;

    /** 状态：1正常 0停用 */
    private Integer status = 1;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
