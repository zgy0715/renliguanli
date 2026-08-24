package com.hrms.repository;

import com.hrms.domain.performance.PerformanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceRecordRepository extends JpaRepository<PerformanceRecord, Long> {

    List<PerformanceRecord> findByEmployeeIdOrderByPeriodDesc(Long employeeId);

    List<PerformanceRecord> findByPeriodOrderByScoreDesc(String period);

    List<PerformanceRecord> findByPeriod(String period);
}
