package com.yuegang.zhihui.ai.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建或修改评估用例的请求模型
 */
public record SaveEvaluationCaseRequest(
        @NotBlank
        @Pattern(regexp = "POLICY|CUSTOMS|TRACEABILITY|RECOMMENDATION")
        String category,                // 用例分类：仅限政策、海关、溯源、推荐四种值
        @NotBlank @Size(max = 4000)
        String question,                // 测试问题文本
        @NotBlank @Size(max = 4000)
        String expectedEvidence,        // 预期结果中必须出现的线索证据
        @Size(max = 4000)
        String forbiddenAnswer,         // 禁止回答的内容
        boolean expectedRefusal,        // 是否预期拒绝回答
        boolean enabled                 // 初始状态是否启用
) {
}