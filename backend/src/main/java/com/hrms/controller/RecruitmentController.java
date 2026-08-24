package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.domain.recruitment.Candidate;
import com.hrms.domain.recruitment.JobRequisition;
import com.hrms.common.BizException;
import com.hrms.repository.CandidateRepository;
import com.hrms.repository.JobRequisitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/recruitment")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
public class RecruitmentController {

    private final CandidateRepository candidateRepository;
    private final JobRequisitionRepository jobRepository;

    @GetMapping("/candidates")
    public ApiResponse<List<Candidate>> candidates(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel) {
        boolean hasChannel = channel != null && !channel.isBlank();
        boolean hasStatus = status != null && !status.isBlank();
        if (hasChannel) {
            List<Candidate> list = candidateRepository.findByChannelOrderByApplyDateDesc(channel);
            if (hasStatus) {
                return ApiResponse.ok(list.stream().filter(c -> status.equals(c.getStatus())).toList());
            }
            return ApiResponse.ok(list);
        }
        if (hasStatus) {
            return ApiResponse.ok(candidateRepository.findByStatusOrderByApplyDateDesc(status));
        }
        return ApiResponse.ok(candidateRepository.findAllByOrderByApplyDateDesc());
    }

    @GetMapping("/jobs")
    public ApiResponse<List<JobRequisition>> jobs() {
        return ApiResponse.ok(jobRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/candidates")
    public ApiResponse<Candidate> createCandidate(@RequestBody Candidate candidate) {
        candidate.setCreatedAt(LocalDateTime.now());
        return ApiResponse.ok(candidateRepository.save(candidate));
    }

    @PutMapping("/candidates/{id}/status")
    public ApiResponse<Candidate> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Candidate c = candidateRepository.findById(id).orElseThrow(() -> new BizException("候选人不存在"));
        c.setStatus(status);
        return ApiResponse.ok(candidateRepository.save(c));
    }
}
