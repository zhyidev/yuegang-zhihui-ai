package com.yuegang.zhihui.ai.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 用户对 AI 回复进行点赞或点踩的请求模型
 */
public record FeedbackRequest(
        @NotBlank String messageId, // 评价的消息 ID，不能为空
        @NotNull Boolean helpful, // 是否有帮助：必填（True/False）
        @Size(max = 1000) String comment // 具体的文字反馈意见：限 1000 字符
) {
}
