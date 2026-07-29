package com.yuegang.zhihui.system.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.system.application.SystemCatalogService;
import com.yuegang.zhihui.system.security.SystemTrustedUserContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 系统目录控制器
@RestController // 表示 REST 控制器
@RequestMapping("/api/v1/system") // 映射系统通用接口路径
public class SystemCatalogController { // 定义最终类：系统目录服务入口
    private final SystemCatalogService service; // 系统目录查询服务
    private final SystemTrustedUserContextResolver users; // 用户上下文解析器

    public SystemCatalogController(SystemCatalogService s, SystemTrustedUserContextResolver u) { // 构造函数
        service = s; // 注入
        users = u; // 注入
    } // 构造结束

    @GetMapping("/dictionaries") // 获取全景已启用字典的接口（业务端展示用）
    ApiResponse<List<DictionaryView>> dictionaries(HttpServletRequest r) { // 请求对象
        users.resolve(r); // 仅要求请求必须经过身份认证
        return ApiResponse.success(service.dictionaries(), TraceIdResolver.resolve(r)); // 返回字典数据
    } // 结束

    @GetMapping("/feature-flags") // 获取所有功能开关状态
    ApiResponse<List<FeatureFlagView>> flags(HttpServletRequest r) { // 请求对象
        users.resolve(r); // 认证校验
        return ApiResponse.success(service.flags(), TraceIdResolver.resolve(r)); // 返回开关列表
    } // 结束

    @PutMapping("/feature-flags/{key}") // 更新具体功能开关配置（通常用于灰度或紧急降级）
    ApiResponse<FeatureFlagView> update(@PathVariable String key, @Valid @RequestBody UpdateFeatureFlagRequest body, HttpServletRequest r) {
        var p = users.resolve(r); // 解析用户
        if (!p.roles().contains("ADMIN")) throw new BusinessException(ErrorCode.PERMISSION_DENIED); // 强制要求管理员操作
        return ApiResponse.success(service.updateFlag(key, body, Long.parseLong(p.userId())), TraceIdResolver.resolve(r)); // 执行更新并返回
    }
}