package com.hrms.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 分页结果
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private List<T> list;
}
