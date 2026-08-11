package com.yuegang.zhihui.notification.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.notification.application.NotificationService;
import com.yuegang.zhihui.notification.security.NotificationSecurity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 该控制器处理通知的内部创建、个人通知查询以及标记已读。(欠标记)
 */
@RestController // 标记为 REST 控制器
public final class NotificationController { // 定义通知核心控制器类
    private final NotificationService service; // 业务服务
    private final NotificationSecurity security; // 安全组件

    // 构造函数依赖注入
    public NotificationController(NotificationService s, NotificationSecurity x) {
        service = s;
        security = x;
    }

    // 辅助方法：封装成功的 ApiResponse 并自动注入追踪 ID
    private static <T> ApiResponse<T> ok(T d, HttpServletRequest r) {
        return ApiResponse.success(d, TraceIdResolver.resolve(r));
    }

    // 内部接口：供其他微服务调用创建新通知
    @PostMapping("/internal/v1/notifications")
    ApiResponse<NotificationView> create(@Valid @RequestBody NotificationCommand c, HttpServletRequest r) {
        security.service(r); // 验证此请求是否来自受信任的内部系统服务
        return ok(service.create(c), r); // 创建通知并返回成功响应
    }

    // 用户接口：获取当前用户自己的通知列表
    @GetMapping("/api/v1/notifications")
    ApiResponse<List<NotificationView>> list(HttpServletRequest r) {
        // 获取当前用户 ID 并查询通知列表
        return ok(service.list(security.user(r)), r);
    }

    // 用户接口：将指定的通知标记为已读
    @PutMapping("/api/v1/notifications/{id}/read")
    ApiResponse<Map<String, Boolean>> read(@PathVariable String id, HttpServletRequest r) {
        service.read(security.user(r), id); // 执行已读标记逻辑
        return ok(Map.of("completed", true), r); // 返回完成状态
    }

}
