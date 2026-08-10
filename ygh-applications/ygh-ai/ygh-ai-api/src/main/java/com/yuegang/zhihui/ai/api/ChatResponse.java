package com.yuegang.zhihui.ai.api;

import java.util.List;

/**
 * AI 回复用户的响应模型
 */
public record ChatResponse(
        String conversationId, // 对话的唯一标识 ID
        String messageId, // 本条回复消息的唯一 ID
        String answer, // AI 生成的回答文本内容
        List<CitationView> citations, // 知识库引证列表（回答依据的文档来源）
        boolean refused, // 是否触发了拒绝回答逻辑（如安全合规拦截）
        String refusalReason // 如果被拒绝，显示拒绝的原因
) {
}