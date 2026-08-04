package com.yuegang.zhihui.notification.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.notification.application.NotificationService;
import com.yuegang.zhihui.notification.security.NotificationSecurity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController // 标识为控制器
@RequestMapping("/api/v1/admin/notifications") // 标记为 REST 控制器并定义管理端基础请求路径
public final class NotificationAdministrationController { // 定义通知管理控制器类
    private final NotificationService service; // 声明通知业务服务
    private final NotificationSecurity security; // 声明安全校验组件

    // 构造函数注入业务服务和安全组件
    public NotificationAdministrationController(NotificationService s, NotificationSecurity x) {
        service = s;
        security = x;
    }

    // 获取发送失败的死信通知列表
    @GetMapping("/dead-letters")
    ApiResponse<List<NotificationDeadLetterView>> dead(HttpServletRequest r) {
        security.require(r, "notification:compensate"); // 校验当前用户是否具备"通知补偿"权限
        // 返回死信列表，并带上当前请求的追踪 ID
        return ApiResponse.success(service.deadLetters(), TraceIdResolver.resolve(r));
    }

    // 重新播放（重试）指定的死信通知
    @PostMapping("/dead-letters/{id}/retry")
    ApiResponse<Map<String, Boolean>> replay(@PathVariable String id, HttpServletRequest r) {
        // 调用重放逻辑，并记录执行人的权限及身份
        service.replay(id, security.require(r, "notification:compensate"));
        // 返回操作已受理的成功响应
        return ApiResponse.success(Map.of("accepted", true),TraceIdResolver.resolve(r));
    }

    // 手动触发待发送通知的分发任务
    @PostMapping("/dispatch")
    ApiResponse<Map<String, Integer>> dispatch(HttpServletRequest r) {
        security.require(r, "notification:compensate"); // 校验"通知补偿"权限
        // 执行待发送通知的分发逻辑，并返回本次发送的数量
        return ApiResponse.success(Map.of("sent", service.dispatchPending()),TraceIdResolver.resolve(r));
    }
}