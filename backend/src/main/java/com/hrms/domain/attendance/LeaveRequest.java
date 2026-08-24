package com.hrms.domain.attendance;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 请假/调休/加班申请
 */
@Data
@Entity
@Table(name = "att_leave")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    /** 类型：年假/事假/病假/婚假/产假/调休/加班 */
    @Column(length = 32)
    private String type;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 时长（天） */
    private Double days;

    /** 原因 */
    @Column(length = 256)
    private String reason;

    /** 审批人（上级） */
    private Long approverId;

    /** 状态：PENDING/APPROVED/REJECTED/CANCELLED */
    @Column(length = 16)
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
