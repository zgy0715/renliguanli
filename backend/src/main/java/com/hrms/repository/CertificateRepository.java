package com.hrms.repository;

import com.hrms.domain.hr.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    List<Certificate> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
