package com.yuegang.zhihui.search.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.search.application.IndexLifecycleService;
import com.yuegang.zhihui.search.security.SearchInternalSecurity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/v1/search/indexes")
public final class IndexLifecycleController {
    private final IndexLifecycleService service;
    private final SearchInternalSecurity security;

    public IndexLifecycleController(IndexLifecycleService s, SearchInternalSecurity x) {
        service = s;
        security = x;
    }

    private static <T> ApiResponse<T> ok(T x, HttpServletRequest r) {
        return ApiResponse.success(x, TraceIdResolver.resolve(r));
    }

    @GetMapping
    ApiResponse<IndexVersionView> status(HttpServletRequest r) {
        security.verify(r);
        return ok(service.status(), r);
    }

    @PostMapping("/{version}")
    ApiResponse<Map<String, Boolean>> create(@PathVariable String version, HttpServletRequest r) {
        security.verify(r);
        service.create(version);
        return ok(Map.of("created", true), r);
    }

    @PutMapping("/active")
    ApiResponse<IndexVersionView> switchTo(@Valid @RequestBody SwitchIndexRequest b, HttpServletRequest r) {
        security.verify(r);
        return ok(service.switchTo(b), r);
    }

    @DeleteMapping("/previous")
    ApiResponse<Map<String, Boolean>> delete(HttpServletRequest r) {
        security.verify(r);
        service.deletePrevious();
        return ok(Map.of("deleted", true), r);
    }
}
