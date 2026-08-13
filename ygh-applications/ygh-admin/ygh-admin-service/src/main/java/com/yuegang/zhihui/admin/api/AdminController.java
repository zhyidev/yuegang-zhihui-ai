package com.yuegang.zhihui.admin.api;

import com.yuegang.zhihui.admin.application.AdminDashboardService;
import com.yuegang.zhihui.admin.security.AdminUserVerifier;
import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public final class AdminController {
    private final AdminDashboardService service;
    private final AdminUserVerifier users;

    public AdminController(AdminDashboardService s, AdminUserVerifier u) {
        service = s;
        users = u;
    }

    @GetMapping("/dashboard")
    ApiResponse<AdminDashboardView> dashboard(HttpServletRequest r) {
        users.verify(r);
        return ApiResponse.success(service.dashboard(), TraceIdResolver.resolve(r));
    }
}
