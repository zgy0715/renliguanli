package com.hrms.domain.recruitment;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 招聘候选人
 */
@Data
@Entity
@Table(name = "recruit_candidate")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String name;

    @Column(length = 32)
    private String phone;

    @Column(length = 128)
    private String email;

    /** 应聘职位 */
    @Column(length = 64)
    private String position;

    /** 招聘渠道：BOSS直聘/拉勾/内推/猎聘/校园招聘/官网 */
    @Column(length = 32)
    private String channel;

    /** 学历 */
    @Column(length = 16)
    private String education;

    /** 工作年限 */
    private Integer workYears;

    /** 简历评分（0-100） */
    private Integer resumeScore;

    /** 状态：简历筛选/初试/复试/HR面/Offer/入职/淘汰 */
    @Column(length = 32)
    private String status;

    /** 申请日期 */
    private LocalDate applyDate;

    /** 备注 */
    @Column(length = 256)
    private String remark;

    private LocalDateTime createdAt;
}
