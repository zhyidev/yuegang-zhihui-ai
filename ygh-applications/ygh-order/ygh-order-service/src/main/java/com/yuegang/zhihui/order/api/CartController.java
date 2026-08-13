package com.yuegang.zhihui.order.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.order.application.CartService;
import com.yuegang.zhihui.order.security.OrderUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cart/items")
public final class CartController {
    private final CartService service;
    private final OrderUserResolver users;

    public CartController(CartService s, OrderUserResolver u) {
        service = s;
        users = u;
    }

    private static <T> ApiResponse<T> ok(T d, HttpServletRequest r) {
        return ApiResponse.success(d, TraceIdResolver.resolve(r));
    }

    @GetMapping
    ApiResponse<List<CartItemView>> list(HttpServletRequest r) {
        return ok(service.list(users.resolve(r)), r);
    }

    @PutMapping
    ApiResponse<CartItemView> save(@Valid @RequestBody CartItemRequest b, HttpServletRequest r) {
        return ok(service.save(users.resolve(r), b), r);
    }

    @DeleteMapping("/{id}")
    ApiResponse<Map<String, Boolean>> delete(@PathVariable String id, @RequestParam long version, HttpServletRequest r) {
        service.delete(users.resolve(r), id, version);
        return ok(Map.of("completed", true), r);
    }
}
