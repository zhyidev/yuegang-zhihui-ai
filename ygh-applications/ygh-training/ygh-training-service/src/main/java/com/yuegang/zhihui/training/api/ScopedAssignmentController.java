package com.yuegang.zhihui.training.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.training.application.ScopedAssignmentService;
import com.yuegang.zhihui.training.security.TrainingUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/training/assignments")
public final class ScopedAssignmentController {
    private final ScopedAssignmentService service;
    private final TrainingUserResolver users;

    public ScopedAssignmentController(ScopedAssignmentService s, TrainingUserResolver u) {
        service = s;
        users = u;
    }

    @PostMapping("/scoped")
    ApiResponse<ScopedAssignmentResult> assign(@Valid @RequestBody CreateScopedAssignmentRequest b, HttpServletRequest r) {
        return ApiResponse.success(service.assign(users.requirePermission(r, "training:assignment:create"), b), TraceIdResolver.resolve(r));
    }
}
