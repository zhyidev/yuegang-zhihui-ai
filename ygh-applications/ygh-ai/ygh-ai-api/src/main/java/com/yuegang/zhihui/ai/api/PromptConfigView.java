package com.yuegang.zhihui.ai.api;

import java.time.OffsetDateTime;

/**
 * 系统后台 AI 提示词（Prompt）配置展示模型
 */
public record PromptConfigView(
    String id, // 配置 ID
    String code, // 配置编码
    String systemPrompt, // 预设的系统提示词内容
    String modelName, // 所使用的模型名称（如 douBao,qwen）
    double temperature, // 模型发散度设置 (0.0 ~ 2.0)
    String knowledgeScope, // 约束的只是范围说明
    String sensitiveWords, // 敏感词的黑名单列表
    boolean enabled, // 当前是否引用
    long version, // 乐观锁版本号
    OffsetDateTime updatedAt // 最后修改时间
) {
}
