package com.hrms.repository;

import com.hrms.domain.employee.Education;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EducationRepository extends JpaRepository<Education, Long> {

    List<Education> findByEmployeeIdOrderByStartDateAsc(Long employeeId);

    void deleteByEmployeeId(Long employeeId);
}
