package com.hrms.repository;

import com.hrms.domain.org.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findAllByOrderByLevelAsc();

    List<Position> findByDeptId(Long deptId);
}
