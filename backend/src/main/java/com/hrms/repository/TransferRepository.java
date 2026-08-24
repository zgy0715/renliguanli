package com.hrms.repository;

import com.hrms.domain.employee.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findByEmployeeIdOrderByEffectiveDateAsc(Long employeeId);

    void deleteByEmployeeId(Long employeeId);
}
