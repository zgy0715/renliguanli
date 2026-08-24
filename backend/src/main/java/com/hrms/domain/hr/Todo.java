package com.hrms.domain.hr;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办事项
 */
@Data
@Entity
@Table(name = "hr_todo", indexes = {
        @Index(name = "idx_todo_assignee", columnList = "assigneeId,status")
})
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String title;

    /** 类型：审批/入职/转正/合同到期/考勤异常/培训 */
    @Column(length = 32)
    private String type;

    /** 处理人 */
    private Long assigneeId;

    /** 业务关联ID（可空） */
    private Long bizId;

    /** 优先级：高/中/低 */
    @Column(length = 8)
    private String priority;

    /** 状态：PENDING/COMPLETED */
    @Column(length = 16)
    private String status;

    private LocalDateTime dueTime;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
