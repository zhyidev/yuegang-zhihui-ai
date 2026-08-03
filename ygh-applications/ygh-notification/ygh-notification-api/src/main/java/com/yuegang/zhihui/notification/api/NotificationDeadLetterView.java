package com.yuegang.zhihui.notification.api;

import java.time.OffsetDateTime;

/**
 * 这个 Record 用于展示死信队列（发送失败）的通知记录
 */
public record NotificationDeadLetterView( // 定义通知死信记录视图类，用于展示失败信息
                                          String id, // 死信记录的唯一标识 ID
                                          String messageId, // 原始信息的 ID
                                          String eventId, // 关联的业务事件 ID
                                          String failureReason, // 发送失败的原因描述
                                          OffsetDateTime failedAt // 发送失败发生的确切时间点
) {
} // 定义类结束