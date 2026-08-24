package com.hrms.domain.hr;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 电子证明（在职/收入证明）
 */
@Data
@Entity
@Table(name = "hr_certificate")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    /** 类型：EMPLOYMENT 在职证明 / INCOME 收入证明 */
    @Column(length = 16)
    private String type;

    /** 用途 */
    @Column(length = 128)
    private String purpose;

    /** 状态：PENDING/ISSUED/REJECTED */
    @Column(length = 16)
    private String status;

    /** 生效日期 */
    private LocalDate effectiveDate;

    private LocalDateTime createdAt;
}
