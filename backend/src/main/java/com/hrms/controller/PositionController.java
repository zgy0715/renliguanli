package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.domain.org.Position;
import com.hrms.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionRepository positionRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiResponse<List<Position>> list() {
        return ApiResponse.ok(positionRepository.findAllByOrderByLevelAsc());
    }
}
