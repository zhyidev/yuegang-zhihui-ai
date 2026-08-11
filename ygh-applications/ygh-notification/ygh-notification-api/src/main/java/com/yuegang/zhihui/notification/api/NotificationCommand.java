package com.yuegang.zhihui.notification.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 这个Record用于封装发送通知的指令请求
 */
public record NotificationCommand( // 定义通知发送指令记录类
                                   @NotBlank String eventId, // 业务事件 ID，不能为空且必须包含非空白字符，用于幂等或溯源
                                   @NotBlank String userId, // 接收通知的用户 ID，不能为空
                                   @NotBlank String templateCode, // 使用通知模板编码，不能为空
                                   @NotNull @Size(max = 50) Map<String, String> variables
                                   // 模板变量 Map，不能为空且最多支持 50 对键值，用于填充模板占位符
) {
} // 定义类结束
