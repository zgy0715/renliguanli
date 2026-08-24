package com.hrms.domain.employee;

import com.hrms.domain.org.Department;
import com.hrms.domain.org.Position;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工全生命周期档案
 */
@Data
@Entity
@Table(name = "emp_employee", indexes = {
        @Index(name = "idx_emp_no", columnList = "employeeNo"),
        @Index(name = "idx_emp_dept", columnList = "departmentId"),
        @Index(name = "idx_emp_status", columnList = "status")
})
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 工号 */
    @Column(nullable = false, unique = true, length = 32)
    private String employeeNo;

    @Column(nullable = false, length = 64)
    private String name;

    /** MALE/FEMALE */
    @Column(length = 16)
    private String gender;

    private LocalDate birthDate;

    /** 身份证号（敏感，接口脱敏） */
    @Column(length = 32)
    private String idCard;

    @Column(length = 32)
    private String phone;

    @Column(length = 128)
    private String email;

    @Column(length = 64)
    private String address;

    /** 头像 */
    @Column(length = 256)
    private String avatar;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "departmentId")
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "positionId")
    private Position position;

    /** 职级：P1-P8 / M1-M5 */
    @Column(length = 16)
    private String level;

    /** 直属上级（员工ID） */
    private Long managerId;

    /** 入职日期 */
    private LocalDate hireDate;

    /** 转正日期 */
    private LocalDate regularDate;

    /** 离职日期 */
    private LocalDate leaveDate;

    /** 员工状态：ACTIVE 在职 / PROBATION 试用 / LEFT 离职 / SUSPEND 停职 */
    @Column(length = 16, nullable = false)
    private String status = "ACTIVE";

    /** 学历：博士/硕士/本科/大专/高中 */
    @Column(length = 16)
    private String education;

    @Column(length = 64)
    private String school;

    @Column(length = 64)
    private String major;

    /** 婚姻状况 */
    @Column(length = 16)
    private String maritalStatus;

    /** 政治面貌 */
    @Column(length = 16)
    private String politicalStatus;

    /** 民族 */
    @Column(length = 16)
    private String nationality;

    /** 籍贯 */
    @Column(length = 32)
    private String nativePlace;

    /** 基本工资（月薪） */
    private BigDecimal baseSalary;

    /** 银行卡号 */
    @Column(length = 32)
    private String bankCard;

    /** 紧急联系人 */
    @Column(length = 32)
    private String emergencyContact;

    @Column(length = 32)
    private String emergencyPhone;

    /** 人才标签，逗号分隔：高潜/核心骨干/待改进/储备干部 */
    @Column(length = 256)
    private String tags;

    /** 人才盘点九宫格位：9A 高潜/9B... */
    @Column(length = 8)
    private String gridCell;

    /** 员工画像评分（0-100） */
    private Integer profileScore;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
