package com.hrms.controller;

import com.hrms.common.ApiResponse;
import com.hrms.dto.AssistantReply;
import com.hrms.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智能助手：自然语言问数（规则式意图引擎）。
 * 所有登录角色可用；敏感数据（薪资/成本）由服务层按角色收敛。
 */
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    public record AskRequest(String question) {
    }

    @PostMapping("/ask")
    public ApiResponse<AssistantReply> ask(@RequestBody AskRequest request) {
        return ApiResponse.ok(assistantService.ask(request.question()));
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> history() {
        return ApiResponse.ok(assistantService.history());
    }

    @DeleteMapping("/history")
    public ApiResponse<Void> clearHistory() {
        assistantService.clearHistory();
        return ApiResponse.ok();
    }
}
