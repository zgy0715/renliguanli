package com.hrms.domain.training;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工学习记录
 */
@Data
@Entity
@Table(name = "train_record", indexes = {
        @Index(name = "idx_trainee", columnList = "employeeId")
})
public class TrainingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;
    private Long courseId;

    /** 状态：进行中/已完成/未通过 */
    @Column(length = 16)
    private String status;

    /** 学习进度（0-100） */
    private Integer progress;

    /** 考试成绩 */
    private Double examScore;

    /** 是否通过考试 */
    private Boolean passed;

    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
}
