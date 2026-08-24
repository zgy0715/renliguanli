package com.hrms.domain.attendance;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 排班方案（多规则考勤）
 */
@Data
@Entity
@Table(name = "att_shift")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String name;

    /** 班次类型：标准/倒班/弹性/大小周/晚班/夜班 */
    @Column(length = 32)
    private String type;

    private LocalTime workStart;
    private LocalTime workEnd;

    /** 标准工时（小时） */
    private Integer standardHours = 8;

    /** 适用工作日：1-5（周一到周五）逗号分隔，7=周日 */
    @Column(length = 32)
    private String workDays;

    /** 迟到宽限分钟数 */
    private Integer lateGraceMinutes = 10;

    /** 是否弹性 */
    private Boolean flexible = false;

    /** 适用部门ID（空=全公司） */
    private Long deptId;

    private Integer status = 1;

    private LocalDateTime createdAt;
}
