package com.yuegang.zhihui.inventory.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.inventory.application.InventoryService;
import com.yuegang.zhihui.inventory.security.InventoryInternalSecurity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1/inventory")
public final class InventoryController {
    private final InventoryService service;
    private final InventoryInternalSecurity security;

    public InventoryController(InventoryService s, InventoryInternalSecurity x) {
        service = s;
        security = x;
    }

    private static <T> ApiResponse<T> ok(T d, HttpServletRequest r) {
        return ApiResponse.success(d, TraceIdResolver.resolve(r));
    }

    @GetMapping("/{sku}")
    ApiResponse<InventoryView> get(@PathVariable String sku, HttpServletRequest r) {
        security.verify(r);
        return ok(service.get(sku), r);
    }

    @PostMapping("/adjust")
    ApiResponse<InventoryView> adjust(@Valid @RequestBody InventoryCommand c, HttpServletRequest r) {
        security.verify(r);
        return ok(service.adjust(c), r);
    }

    @PostMapping("/reserve")
    ApiResponse<InventoryView> reserve(@Valid @RequestBody InventoryCommand c, HttpServletRequest r) {
        security.verify(r);
        return ok(service.reserve(c), r);
    }

    @PostMapping("/confirm")
    ApiResponse<InventoryView> confirm(@Valid @RequestBody InventoryCommand c, HttpServletRequest r) {
        security.verify(r);
        return ok(service.confirm(c), r);
    }

    @PostMapping("/release")
    ApiResponse<InventoryView> release(@Valid @RequestBody InventoryCommand c, HttpServletRequest r) {
        security.verify(r);
        return ok(service.release(c), r);
    }
}
