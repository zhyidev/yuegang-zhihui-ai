package com.yuegang.zhihui.notification.api;

import java.time.OffsetDateTime;

/**
 * 这个 Record 用于展示通知模板的详细信息
 */
public record NotificationTemplateView( // 发送通知模板视图类，用于后台管理展示
                                        String code, // 模板唯一编码（如：USER_REGISTRATION）
                                        String titleTemplate, // 标题模板内容，支持占位符
                                        String contentTemplate, // 正文模板内容，支持占位符
                                        String channel, // 通信渠道（例如：IN_APP 站内信）
                                        boolean enabled, // 模板是否处于启用状态
                                        long version) {
} // 乐观锁版本号，用于防止并发修改冲突