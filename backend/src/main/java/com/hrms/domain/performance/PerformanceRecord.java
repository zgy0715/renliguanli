package com.hrms.domain.performance;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 绩效评估记录
 */
@Data
@Entity
@Table(name = "perf_record", indexes = {
        @Index(name = "idx_perf_emp", columnList = "employeeId")
})
public class PerformanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    /** 考核周期：2026-Q2 / 2026-H1 / 2026年度 */
    @Column(length = 16)
    private String period;

    /** 考核模式：OKR / KPI / 360 */
    @Column(length = 16)
    private String mode;

    /** 得分（0-100） */
    private Double score;

    /** 评级：S/A/B/C/D */
    @Column(length = 4)
    private String rating;

    /** 状态：DRAFT/SUBMITTED/APPROVED */
    @Column(length = 16)
    private String status;

    /** 面谈记录 */
    @Column(length = 512)
    private String interviewNote;

    /** 评价人 */
    private Long evaluatorId;

    private LocalDateTime createdAt;
}
