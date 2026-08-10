package com.yuegang.zhihui.ai.api;

/**
 * 用于一键 启用/禁用 评估用例的精简请求模型
 */
public record SetEvaluationCaseStatusRequest(
        boolean enabled // 目标状态：True为启用，False 为禁用
) {
}