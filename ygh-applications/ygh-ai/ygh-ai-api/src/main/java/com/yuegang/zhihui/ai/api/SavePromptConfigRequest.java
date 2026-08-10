package com.yuegang.zhihui.ai.api;

import jakarta.validation.constraints.*;

/**
 * 管理员修改 AI 模型行为为参数的请求模型
 */
public record SavePromptConfigRequest(
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,63}") String code, // 配置编码：限大写字母、数字下划线、且首字母必须为大写
        @NotBlank @Size(max = 12000) String systemPrompt, // 核心系统提示词：最大支持 12000 字符
        @NotBlank @Size(max = 128) String modelName, // 调用的 AI 模型名称
        @DecimalMin("0.0") @DecimalMax("2.0") double temperature, // 温度系数：限制在 0.0 到 2.0 之间
        @Size(max = 2000) String knowledgeScope, // 定义 AI 的知识服务范畴
        @Size(max = 2000) String sensitiveWords, // 配置需要拦截的敏感词过滤列表
        boolean enabled, // 是否设为启用
        long version // 进行更新操作时必须传入当前版本号实现
) {
}