package com.hrms.repository;

import com.hrms.domain.hr.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByAssigneeIdAndStatusOrderByPriorityAscCreatedAtDesc(Long assigneeId, String status);

    List<Todo> findByStatus(String status);
}
