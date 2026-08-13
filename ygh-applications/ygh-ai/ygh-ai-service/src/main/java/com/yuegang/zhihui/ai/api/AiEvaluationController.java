package com.yuegang.zhihui.ai.api;

import com.yuegang.zhihui.ai.application.AiEvaluationService;
import com.yuegang.zhihui.ai.security.AiUserResolver;
import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/ai")
public final class AiEvaluationController {
    private final AiEvaluationService service;
    private final AiUserResolver users;

    public AiEvaluationController(AiEvaluationService s, AiUserResolver u) {
        service = s;
        users = u;
    }

    @GetMapping("/summary")
    ApiResponse<AiAdminSummaryView> summary(HttpServletRequest r) {
        users.require(r, "ai:audit:read");
        return ApiResponse.success(service.summary(), TraceIdResolver.resolve(r));
    }

    @PostMapping("/evaluations/run")
    ApiResponse<List<EvaluationRunView>> run(HttpServletRequest r) {
        return ApiResponse.success(service.run(users.require(r, "ai:evaluation:execute")), TraceIdResolver.resolve(r));
    }
}
