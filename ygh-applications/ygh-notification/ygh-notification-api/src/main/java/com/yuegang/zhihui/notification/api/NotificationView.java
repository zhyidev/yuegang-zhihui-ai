package com.yuegang.zhihui.notification.api;

import java.time.OffsetDateTime;

/**
 * 这个 Record 用于展示给用户看的具体通知内容
 */
public record NotificationView( // 定义通知内容视图类，用于站内信列表展示
                                String id, // 通知记录唯一标识 ID
                                String title, // 渲染后的最终通知标题
                                String content, // 渲染后的最终通知正文内容
                                String status, // 通知的当前状态（如：PENDING, SENT）
                                boolean read, // 用户是否已读标志
                                OffsetDateTime createdAt // 通知的创建时间点
) {
}
