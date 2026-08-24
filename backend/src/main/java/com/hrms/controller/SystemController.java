package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.domain.system.DictItem;
import com.hrms.domain.system.OperationLog;
import com.hrms.domain.system.User;
import com.hrms.repository.DictItemRepository;
import com.hrms.repository.OperationLogRepository;
import com.hrms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemController {

    private final UserRepository userRepository;
    private final OperationLogRepository logRepository;
    private final DictItemRepository dictRepository;

    @GetMapping("/users")
    public ApiResponse<List<User>> users() {
        List<User> users = userRepository.findAll();
        users.forEach(u -> u.setPassword("******"));
        return ApiResponse.ok(users);
    }

    @GetMapping("/logs")
    public ApiResponse<List<OperationLog>> logs() {
        return ApiResponse.ok(logRepository.findAll(Sort.by(Sort.Direction.DESC, "id")));
    }

    @GetMapping("/dicts")
    public ApiResponse<List<DictItem>> dicts(@RequestParam(required = false) String type) {
        if (type == null || type.isBlank()) {
            return ApiResponse.ok(dictRepository.findAll(Sort.by(Sort.Direction.ASC, "type", "sort")));
        }
        return ApiResponse.ok(dictRepository.findByTypeOrderBySortAsc(type));
    }
}
