package com.hrms.domain.system;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 数据字典项
 */
@Data
@Entity
@Table(name = "sys_dict")
public class DictItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 字典类型：gender/education/attendance_status/payroll_status/contract_type 等 */
    @Column(length = 64, nullable = false)
    private String type;

    @Column(length = 32, nullable = false)
    private String label;

    @Column(length = 64, nullable = false)
    private String value;

    /** 排序 */
    private Integer sort;

    /** 状态：1启用 0停用 */
    private Integer status = 1;
}
