package com.hrms.repository;

import com.hrms.domain.attendance.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    List<Shift> findByStatusOrderByIdAsc(Integer status);
}
