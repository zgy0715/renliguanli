package com.hrms.domain.employee;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工异动记录（调岗/晋升/转正/调薪/离职）
 */
@Data
@Entity
@Table(name = "emp_transfer")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    /** 异动类型：TRANSFER 调岗 / PROMOTE 晋升 / REGULAR 转正 / SALARY 调薪 / LEAVE 离职 */
    @Column(length = 32)
    private String type;

    /** 原部门 */
    private Long fromDeptId;
    /** 新部门 */
    private Long toDeptId;

    /** 原职位 */
    private Long fromPositionId;
    /** 新职位 */
    private Long toPositionId;

    /** 原职级 */
    @Column(length = 16)
    private String fromLevel;
    /** 新职级 */
    @Column(length = 16)
    private String toLevel;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 原因说明 */
    @Column(length = 256)
    private String reason;

    /** 办理人 */
    private Long operatorId;

    private LocalDateTime createdAt;
}
