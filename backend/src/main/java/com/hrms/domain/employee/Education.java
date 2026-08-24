package com.hrms.domain.employee;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教育经历
 */
@Data
@Entity
@Table(name = "emp_education")
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employeeId;

    @Column(length = 16)
    private String degree;

    @Column(length = 64)
    private String school;

    @Column(length = 64)
    private String major;

    private LocalDate startDate;
    private LocalDate endDate;

    private LocalDateTime createdAt;
}
