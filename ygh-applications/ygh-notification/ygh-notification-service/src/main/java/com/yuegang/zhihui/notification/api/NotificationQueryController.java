package com.yuegang.zhihui.notification.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.notification.application.NotificationQueryService;
import com.yuegang.zhihui.notification.security.NotificationSecurity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 该控制器处理关于未读通知状态的查询
 */
@RestController // 标记为 REST 控制器
public final class NotificationQueryController { // 定义通知查询的控制器类
    private final NotificationQueryService service; // 查询专用服务
    private final NotificationSecurity security; // 安组组件

    // 构造函数注入
    public NotificationQueryController(NotificationQueryService s, NotificationSecurity x) {
        service = s;
        security = x;
    }

    // 获取当前用户的未读通知总数
    @GetMapping("/api/v1/notifications/unread-count")
    ApiResponse<Map<String, Long>> unread(HttpServletRequest r) {
        // 解析用户身份并统计未读数
        return ApiResponse.success(Map.of("count", service.unread(security.user(r))), TraceIdResolver.resolve(r));
    }

    // 将当前用户的所有通知一键标记为已读
    @PutMapping("/api/v1/notifications/read-all")
    ApiResponse<Map<String, Integer>> readAll(HttpServletRequest r) {
        // 解析用户身份并更新所有消息状态，返回受影响的行数
        return ApiResponse.success(Map.of("updated", service.readAll(security.user(r))), TraceIdResolver.resolve(r));
    }
}