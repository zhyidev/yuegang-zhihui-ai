package com.yuegang.zhihui.ai.domain;

import java.util.List;

/**
 * AI 模型调用网关接口，定义与外部大模型交互的标准
 */
public interface ModelGateway {
    // 根据系统提示词和用户提示词获取模型的文本回答
    String answer(String systemPrompt, String userPrompt);

    // 获取回答并附带参考来源，默认实现为仅返回文本且来源为空列表
    default ModelAnswer answerWithSources(String systemPrompt, String userPrompt) {
        return new ModelAnswer(answer(systemPrompt, userPrompt), List.of());
    }

    // 返回当前使用的模型名称，默认为 "unknow"
    default String modelName() {
        return "unknow";
    }

    // 检查模型当前是否可用，默认为可用
    default boolean available() {
        return true;
    }

    // 检查模型是否支持互联网搜索功能，默认为不支持
    default boolean supportsWebSearch() {
        return false;
    }
}
