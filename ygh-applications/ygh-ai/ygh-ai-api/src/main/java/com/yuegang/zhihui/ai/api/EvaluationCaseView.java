package com.yuegang.zhihui.ai.api;

/**
 * AI 自动评估测试用例的展示模型
 */
public record EvaluationCaseView(
        String id,            // 用户 ID
        String category,      // 评估分类
        String question,      // 测试问题
        String expectedEvidence, // 预期回答中应包含的关键词证据或关键词
        String forbiddenAnswer,  // 回答中严禁出现的词汇（如竞争对手名）
        String expectedResidual,   // 是否预期 AI 应该拒绝回答（测试安全边界）
        boolean enabled       // 该测试用例是否处于启用状态
) {
}