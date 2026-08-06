package com.yuegang.zhihui.notification.application;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时任务类，负责周期性扫描并分发待发送的通知
 */
public final class NotificationDispatchJob { // 定义分发任何类
    private final NotificationService service; // 依赖通知核心服务

    public NotificationDispatchJob(NotificationService s) { // 构造函数注入
        service = s;
    }

    // 设置定时任务，默认 5 秒执行一次，支持配置文件重写延时
    @Scheduled(fixedDelayString = "${ygh.notification.dispatch-deplay-ms:5000}")
    public void dispatch() { // 定时执行方法
        service.dispatchPending(); // 调用服务类方法处理待发送的消息
    }
}