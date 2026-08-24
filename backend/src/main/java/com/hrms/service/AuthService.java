package com.hrms.service;

import com.hrms.common.BizException;
import com.hrms.domain.system.User;
import com.hrms.dto.LoginRequest;
import com.hrms.dto.LoginResponse;
import com.hrms.repository.UserRepository;
import com.hrms.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BizException(401, "用户名或密码错误"));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole(), user.getEmployeeId());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRole(), user.getEmployeeId(), jwtUtil.getExpire());
    }
}
