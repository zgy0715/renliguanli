package com.hrms.domain.training;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 培训课程 / 企业课程库
 */
@Data
@Entity
@Table(name = "train_course")
public class TrainingCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    /** 分类：新人入职/专业技能/管理能力/通用素养/合规 */
    @Column(length = 32)
    private String category;

    /** 讲师 */
    @Column(length = 64)
    private String teacher;

    /** 时长（小时） */
    private Double hours;

    /** 是否线上 */
    private Boolean online = true;

    /** 累计学习人次 */
    private Integer learnerCount = 0;

    /** 平均评分（0-5） */
    private Double avgRating;

    /** 状态：上架/下架 */
    @Column(length = 16)
    private String status;

    /** 简介 */
    @Column(length = 512)
    private String description;

    private LocalDateTime createdAt;
}
