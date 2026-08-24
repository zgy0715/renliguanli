package com.hrms.domain.payroll;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度薪酬核算记录（一员工一月一条）
 */
@Data
@Entity
@Table(name = "pay_record", indexes = {
        @Index(name = "idx_pay_month", columnList = "payMonth"),
        @Index(name = "idx_pay_emp", columnList = "employeeId")
})
public class PayrollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    /** 核算月份：2026-07 */
    @Column(length = 8, nullable = false)
    private String payMonth;

    // ===== 应发项 =====
    /** 基本工资 */
    private BigDecimal baseSalary;
    /** 岗位工资 */
    private BigDecimal positionSalary;
    /** 绩效工资 */
    private BigDecimal performanceSalary;
    /** 技能/补贴 */
    private BigDecimal allowance;
    /** 加班费 */
    private BigDecimal overtimePay;
    /** 奖金 */
    private BigDecimal bonus;

    /** 应发合计 */
    private BigDecimal grossAmount;

    // ===== 扣减项 =====
    /** 五险一金-养老 */
    private BigDecimal socialPension;
    /** 五险一金-医疗 */
    private BigDecimal socialMedical;
    /** 五险一金-失业 */
    private BigDecimal socialUnemployment;
    /** 公积金 */
    private BigDecimal housingFund;
    /** 个税（累计预扣） */
    private BigDecimal tax;
    /** 其他扣款 */
    private BigDecimal deduction;

    /** 扣减合计 */
    private BigDecimal deductionTotal;

    /** 实发合计 */
    private BigDecimal netAmount;

    /** 核算状态：DRAFT 草稿 / CALCULATED 已核算 / CONFIRMED 已确认 / PAID 已发放 */
    @Column(length = 16)
    private String status;

    /** 核算人 */
    private Long operatorId;

    /** 发放日期 */
    private java.time.LocalDate payDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
