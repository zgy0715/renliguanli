package com.hrms.repository;

import com.hrms.domain.payroll.PayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {

    List<PayrollRecord> findByPayMonthOrderByEmployeeIdAsc(String payMonth);

    Optional<PayrollRecord> findByEmployeeIdAndPayMonth(Long employeeId, String payMonth);

    List<PayrollRecord> findByPayMonth(String payMonth);

    List<PayrollRecord> findByEmployeeIdOrderByPayMonthDesc(Long employeeId);

    List<PayrollRecord> findByPayMonthInOrderByPayMonthAsc(List<String> months);
}
