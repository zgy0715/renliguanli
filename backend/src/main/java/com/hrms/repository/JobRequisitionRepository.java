package com.hrms.repository;

import com.hrms.domain.recruitment.JobRequisition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRequisitionRepository extends JpaRepository<JobRequisition, Long> {

    List<JobRequisition> findByStatusOrderByPriorityAsc(String status);

    List<JobRequisition> findAllByOrderByCreatedAtDesc();
}
