package com.yuegang.zhihui.order.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.order.application.CommerceReconciliationService;
import com.yuegang.zhihui.order.security.OrderUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
public final class CommerceReconciliationController {
    private final CommerceReconciliationService service;
    private final OrderUserResolver users;

    public CommerceReconciliationController(CommerceReconciliationService s, OrderUserResolver u) {
        service = s;
        users = u;
    }

    @PostMapping("/reconciliation")
    ApiResponse<CommerceReconciliationView> run(HttpServletRequest r) {
        users.requirePermission(r, "order:reconcile");
        return ApiResponse.success(service.run(), TraceIdResolver.resolve(r));
    }
}
