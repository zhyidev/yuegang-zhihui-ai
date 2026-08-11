package com.yuegang.zhihui.notification.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.notification.application.NotificationTemplateService;
import com.yuegang.zhihui.notification.security.NotificationSecurity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 该控制器处理通知模板的管理，包括查看和编辑
 */
@RestController
@RequestMapping("/api/v1/admin/notifications/templates") // 定义通知模板管理的基础路径
public class NotificationTemplateController { // 定义模板管理控制器类
    private final NotificationTemplateService service; // 声明模板业务服务
    private final NotificationSecurity security; // 声明安全组件

    // 检查函数注入依赖
    public NotificationTemplateController(NotificationTemplateService service, NotificationSecurity security) {
        this.service = service;
        this.security = security;
    }

    // 获取所有通知模板列表
    @GetMapping
    ApiResponse<List<NotificationTemplateView>> list(HttpServletRequest request) {
        security.require(request, "notification:template:write"); // 校验"通知模板写入"权限
        return ApiResponse.success(service.list(), TraceIdResolver.resolve(request)); // 返回模板列表
    }

    // 创建或更新通知模板
    @PutMapping("/{code}")
    ApiResponse<NotificationTemplateView> save(@PathVariable String code,
                                               @Valid @RequestBody SaveNotificationTemplateRequest body,
                                               HttpServletRequest request) {
        security.require(request, "notification:template:write"); // 校验"通知模板写入"权限
        // 保存模板内容并返回保存后的模板视图
        return ApiResponse.success(service.save(code, body), TraceIdResolver.resolve(request));
    }
}
