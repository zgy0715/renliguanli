package com.hrms.repository;

import com.hrms.domain.training.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long> {

    List<TrainingRecord> findByEmployeeIdOrderByEnrolledAtDesc(Long employeeId);

    long countByStatus(String status);

    long countDistinctEmployeeIdByStatus(String status);
}
