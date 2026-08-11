package com.yuegang.zhihui.ai.api;

import com.yuegang.zhihui.ai.application.AiGovernanceService;
import com.yuegang.zhihui.ai.security.AiUserResolver;
import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/ai")
public final class AiGovernanceController {
    private final AiGovernanceService service;
    private final AiUserResolver users;

    public AiGovernanceController(AiGovernanceService s, AiUserResolver u) {
        service = s;
        users = u;
    }

    private static <T> ApiResponse<T> ok(T x, HttpServletRequest r) {
        return ApiResponse.success(x, TraceIdResolver.resolve(r));
    }

    @GetMapping("/prompts")
    ApiResponse<List<PromptConfigView>> prompts(HttpServletRequest r) {
        users.require(r, "ai:prompt:read");
        return ok(service.prompts(), r);
    }

    @PostMapping("/prompts")
    ApiResponse<PromptConfigView> save(@Valid @RequestBody SavePromptConfigRequest b, HttpServletRequest r) {
        return ok(service.save(b, users.require(r, "ai:prompt:write")), r);
    }

    @GetMapping("/evaluation-cases")
    ApiResponse<List<EvaluationCaseView>> cases(HttpServletRequest r) {
        users.require(r, "ai:evaluation:read");
        return ok(service.cases(), r);
    }

    @PostMapping("/evaluation-cases")
    ApiResponse<EvaluationCaseView> add(@Valid @RequestBody SaveEvaluationCaseRequest b, HttpServletRequest r) {
        return ok(service.addCase(b, users.require(r, "ai:evaluation:write")), r);
    }

    @PutMapping("/evaluation-cases/{id}/status")
    ApiResponse<EvaluationCaseView> status(@PathVariable String id, @RequestBody SetEvaluationCaseStatusRequest b, HttpServletRequest r) {
        users.require(r, "ai:evaluation:write");
        return ok(service.setCaseEnabled(id, b.enabled()), r);
    }
}
