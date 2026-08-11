package com.yuegang.zhihui.ai.api;

import java.time.OffsetDateTime;

/**
 * 历史对话列表的简要信息
 */
public record ConversationView(
        String id,            // 对话 ID
        String title,         // 对话标题（通常取第一条问题的摘要）
        String status,        // 对话状态（如：ACTIVE，ARCHIVED）
        OffsetDateTime updateAt // 最后一次互动的时间
) {
}
