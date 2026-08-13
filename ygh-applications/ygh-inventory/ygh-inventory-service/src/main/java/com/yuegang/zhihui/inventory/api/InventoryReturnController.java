package com.yuegang.zhihui.inventory.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.inventory.application.InventoryReturnService;
import com.yuegang.zhihui.inventory.security.InventoryInternalSecurity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/inventory")
public final class InventoryReturnController {
    private final InventoryReturnService service;
    private final InventoryInternalSecurity security;

    public InventoryReturnController(InventoryReturnService s, InventoryInternalSecurity x) {
        service = s;
        security = x;
    }

    @PostMapping("/return-sold")
    ApiResponse<InventoryView> returned(@Valid @RequestBody InventoryCommand b, HttpServletRequest r) {
        security.verify(r);
        return ApiResponse.success(service.returnSold(b), TraceIdResolver.resolve(r));
    }
}
