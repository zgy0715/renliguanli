package com.hrms.domain.employee;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 劳动合同
 */
@Data
@Entity
@Table(name = "emp_contract")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    /** 合同类型：固定期限/无固定期限/实习/劳务 */
    @Column(length = 32)
    private String type;

    /** 合同编号 */
    @Column(length = 64)
    private String contractNo;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate signDate;

    /** 状态：有效/即将到期/已到期/已终止 */
    @Column(length = 16)
    private String status;

    /** 是否电子签章 */
    private Boolean eSigned = false;

    private LocalDateTime createdAt;
}
