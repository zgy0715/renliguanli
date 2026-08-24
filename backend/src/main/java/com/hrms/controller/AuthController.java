package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.common.BizException;
import com.hrms.common.SecurityUtil;
import com.hrms.domain.system.User;
import com.hrms.dto.LoginRequest;
import com.hrms.dto.LoginResponse;
import com.hrms.dto.UserInfo;
import com.hrms.repository.UserRepository;
import com.hrms.security.LoginUser;
import com.hrms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfo> me() {
        LoginUser loginUser = SecurityUtil.currentUser();
        User user = userRepository.findById(loginUser.id())
                .orElseThrow(() -> new BizException(401, "登录已过期，请重新登录"));
        UserInfo info = new UserInfo(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRole(), user.getEmployeeId(),
                (user.getId() * 37) % 10);
        return ApiResponse.ok(info);
    }
}
