package com.yuegang.zhihui.ai.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户发起 AI 对话的请求模型
 */
public record ChatRequest(
    String conversationId, // 对话 ID（如果是新对话则为空，继续对话则需传入）
    @NotBlank @Size(max = 4000) String message, // 用户输入的咨询信息：不能为空且限 4000 字符
    String category, // 咨询分类（如：政策、报关、溯源等）
    boolean includeOwnOrders // 是否允许 AI 关联读取用户自己的订单数据
) {
}
