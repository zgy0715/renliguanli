package com.hrms.repository;

import com.hrms.domain.employee.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long>,
        JpaSpecificationExecutor<Employee> {

    long countByDepartmentId(Long deptId);

    long countByStatus(String status);

    long countByDepartmentIdAndStatus(Long deptId, String status);

    boolean existsByEmployeeNo(String employeeNo);

    List<Employee> findByStatusNotOrderByHireDateAsc(String status);

    List<Employee> findByStatus(String status);
}
