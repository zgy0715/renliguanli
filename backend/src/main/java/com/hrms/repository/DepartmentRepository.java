package com.hrms.repository;

import com.hrms.domain.org.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByStatusOrderBySortAsc(Integer status);

    List<Department> findAllByOrderBySortAsc();

    List<Department> findByParentId(Long parentId);
}
