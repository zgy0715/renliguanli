package com.hrms.repository;

import com.hrms.domain.employee.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByEmployeeIdOrderByIdAsc(Long employeeId);

    void deleteByEmployeeId(Long employeeId);
}
