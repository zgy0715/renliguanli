package com.hrms.domain.recruitment;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 招聘需求（岗位编制）
 */
@Data
@Entity
@Table(name = "recruit_job")
public class JobRequisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String title;

    private Long deptId;

    /** 招聘人数 */
    private Integer headcount;

    /** 已入职人数 */
    private Integer filled = 0;

    /** 状态：OPEN 招聘中 / CLOSED 已关闭 / FILLED 已满编 */
    @Column(length = 16)
    private String status;

    /** 优先级：高/中/低 */
    @Column(length = 8)
    private String priority;

    private LocalDateTime createdAt;
}
