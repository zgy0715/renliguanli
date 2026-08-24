package com.hrms.domain.system;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志（全留痕审计）
 */
@Data
@Entity
@Table(name = "sys_operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long operatorId;
    private String operatorName;

    /** 模块：员工档案/考勤/薪酬... */
    private String module;

    /** 操作类型：CREATE/UPDATE/DELETE/LOGIN/EXPORT */
    private String action;

    private String description;

    private String ip;

    private LocalDateTime createdAt;
}
