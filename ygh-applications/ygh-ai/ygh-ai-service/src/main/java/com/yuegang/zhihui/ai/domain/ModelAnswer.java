package com.yuegang.zhihui.ai.domain;

import java.util.List;

/**
 * 模型回答及模型工具产生的可审计的公开来源
 */
public record ModelAnswer(String text, List<ModelSource> sources) {
    // 紧凑构造函数，用于参数校验和处理
    public ModelAnswer {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
