package com.yuegang.zhihui.training.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.training.application.TrainingProgressService;
import com.yuegang.zhihui.training.security.TrainingUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/training/progress")
public final class TrainingProgressController {
    private final TrainingProgressService service;
    private final TrainingUserResolver users;

    public TrainingProgressController(TrainingProgressService s, TrainingUserResolver u) {
        service = s;
        users = u;
    }

    private static <T> ApiResponse<T> ok(T d, HttpServletRequest r) {
        return ApiResponse.success(d, TraceIdResolver.resolve(r));
    }

    @PostMapping("/heartbeat")
    ApiResponse<ProgressView> heartbeat(@Valid @RequestBody LearningHeartbeat h, HttpServletRequest r) {
        return ok(service.heartbeat(users.resolve(r), h), r);
    }

    @GetMapping("/{assignment}")
    ApiResponse<ProgressView> get(@PathVariable String assignment, HttpServletRequest r) {
        return ok(service.get(users.resolve(r), assignment), r);
    }
}
