package com.yuegang.zhihui.knowledge.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ReviewKnowledgeRequest(@NotNull Decision decision, @Size(max = 500) String comment,
                                     @PositiveOrZero long version) {
    public enum Decision {APPROVE, REJECT}
}
