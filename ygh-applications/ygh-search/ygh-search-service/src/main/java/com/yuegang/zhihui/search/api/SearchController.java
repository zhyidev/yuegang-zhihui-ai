package com.yuegang.zhihui.search.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.search.application.HybridSearchService;
import com.yuegang.zhihui.search.security.SearchInternalSecurity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/v1/search")
public final class SearchController {
    private final HybridSearchService service;
    private final SearchInternalSecurity security;

    public SearchController(HybridSearchService s, SearchInternalSecurity x) {
        service = s;
        security = x;
    }

    @PostMapping("/hybrid")
    ApiResponse<List<SearchHit>> search(@Valid @RequestBody SearchRequest body, HttpServletRequest r) {
        security.verify(r);
        return ApiResponse.success(service.search(body), TraceIdResolver.resolve(r));
    }

    @PostMapping("/index")
    ApiResponse<Map<String, Boolean>> index(@Valid @RequestBody IndexChunkCommand body, HttpServletRequest r) {
        security.verify(r);
        service.index(body);
        return ApiResponse.success(Map.of("completed", true), TraceIdResolver.resolve(r));
    }
}
