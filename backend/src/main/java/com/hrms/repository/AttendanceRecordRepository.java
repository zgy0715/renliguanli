package com.hrms.repository;

import com.hrms.domain.attendance.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findByWorkDateBetweenOrderByWorkDateAsc(LocalDate start, LocalDate end);

    List<AttendanceRecord> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(Long employeeId, LocalDate start, LocalDate end);

    List<AttendanceRecord> findByWorkDateAndStatus(LocalDate date, String status);
}
