package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.domain.training.TrainingCourse;
import com.hrms.domain.training.TrainingRecord;
import com.hrms.repository.TrainingCourseRepository;
import com.hrms.repository.TrainingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingCourseRepository courseRepository;
    private final TrainingRecordRepository recordRepository;

    @GetMapping("/courses")
    public ApiResponse<List<TrainingCourse>> courses() {
        return ApiResponse.ok(courseRepository.findAll());
    }

    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> records(@RequestParam(required = false) Long employeeId) {
        List<TrainingRecord> records = (employeeId == null)
                ? recordRepository.findAll()
                : recordRepository.findByEmployeeIdOrderByEnrolledAtDesc(employeeId);
        Map<Long, String> courseNames = courseRepository.findAll().stream()
                .collect(Collectors.toMap(TrainingCourse::getId, TrainingCourse::getName, (a, b) -> a));
        List<Map<String, Object>> rows = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("employeeId", r.getEmployeeId());
            m.put("courseName", courseNames.getOrDefault(r.getCourseId(), "-"));
            m.put("status", r.getStatus());
            m.put("progress", r.getProgress());
            m.put("examScore", r.getExamScore());
            m.put("passed", r.getPassed());
            m.put("completedAt", r.getCompletedAt());
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.ok(rows);
    }

    @PostMapping("/courses")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ApiResponse<TrainingCourse> createCourse(@RequestBody TrainingCourse course) {
        course.setCreatedAt(LocalDateTime.now());
        return ApiResponse.ok(courseRepository.save(course));
    }
}
