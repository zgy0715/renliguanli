package com.hrms.repository;

import com.hrms.domain.training.TrainingCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingCourseRepository extends JpaRepository<TrainingCourse, Long> {

    List<TrainingCourse> findByStatusOrderByLearnerCountDesc(String status);
}
