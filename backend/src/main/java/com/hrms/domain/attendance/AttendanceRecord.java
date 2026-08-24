package com.hrms.domain.attendance;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤打卡记录（每日）
 */
@Data
@Entity
@Table(name = "att_record", indexes = {
        @Index(name = "idx_att_emp_date", columnList = "employeeId,workDate"),
        @Index(name = "idx_att_month", columnList = "workDate")
})
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    private LocalDate workDate;

    private java.time.LocalTime checkIn;
    private java.time.LocalTime checkOut;

    /** 打卡方式：WIFI/定位/人脸/系统登录 */
    @Column(length = 16)
    private String method;

    /** 状态：NORMAL 正常 / LATE 迟到 / EARLY 早退 / ABSENT 缺勤 / LEAVE 请假 / FIELD 外勤 / OVERTIME 加班 */
    @Column(length = 16)
    private String status;

    /** 出勤时长（小时） */
    private Double workHours;

    /** 加班时长（小时） */
    private Double overtimeHours;

    /** 异常说明 */
    @Column(length = 128)
    private String remark;

    /** 是否已确认（异常考勤员工确认） */
    private Boolean confirmed = true;

    private LocalDateTime createdAt;
}
