package com.hrms.repository;

import com.hrms.domain.recruitment.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    List<Candidate> findByStatusOrderByApplyDateDesc(String status);

    List<Candidate> findByChannelOrderByApplyDateDesc(String channel);

    List<Candidate> findAllByOrderByApplyDateDesc();
}
