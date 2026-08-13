package com.yuegang.zhihui.ai.api;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 对话详情中的单挑消息模型（支持用户和 AI 角色）
 */
public record MessageView(
    String id, // 消息 ID
    String role, // 角色（USER-用户，ASSISTANT-助手）
    String content, // 消息内容
    boolean refused, // 该消息是否被 AI 系统拒绝发送
    OffsetDateTime createdAt, // 消息发送时间
    List<CitationView> citations // 本条消息关联的引证
) {
    // 紧凑构造函数：确保引证列表不可变的安全副本
    public MessageView {
        citations = List.copyOf(citations);
    }
}
