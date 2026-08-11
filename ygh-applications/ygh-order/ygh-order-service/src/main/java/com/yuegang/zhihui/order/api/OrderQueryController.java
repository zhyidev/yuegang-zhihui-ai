package com.yuegang.zhihui.order.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.order.application.OrderQueryService;
import com.yuegang.zhihui.order.security.OrderUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public final class OrderQueryController {
    private final OrderQueryService service;
    private final OrderUserResolver users;

    public OrderQueryController(OrderQueryService s, OrderUserResolver u) {
        service = s;
        users = u;
    }

    @GetMapping("/api/v1/orders")
    ApiResponse<List<OrderView>> mine(@RequestParam(required = false) String status, @RequestParam(defaultValue = "20") int limit, HttpServletRequest r) {
        return ApiResponse.success(service.mine(users.resolve(r), status, limit), TraceIdResolver.resolve(r));
    }

    @GetMapping("/api/v1/admin/orders")
    ApiResponse<List<OrderView>> admin(@RequestParam(required = false) String status, @RequestParam(defaultValue = "50") int limit, HttpServletRequest r) {
        users.requirePermission(r, "order:read:all");
        return ApiResponse.success(service.admin(status, limit), TraceIdResolver.resolve(r));
    }
}
