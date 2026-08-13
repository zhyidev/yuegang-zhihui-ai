package com.yuegang.zhihui.ai.api;

/**
 * 单次 AI 自动评估运行后的得分详情
 */
public record EvaluationRunView(
    String runId,           // 运行批次 ID
    String caseId,          // 关联的评估用户 ID
    boolean passed,         // 是否通过测试
    double score,           // 回答的质量评分（如 0.0 ~ 1.0）
    int citationCount,      // AI 给出回答时引用的文献数量
    long durationMs,        // 生成回复消耗的毫秒数
    String failureReason    // 如果未通过，记录失败的详细原因
) {
}
