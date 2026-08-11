package com.yuegang.zhihui.training.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record SubmitQuizRequest(@NotBlank String assignmentId, @NotBlank String gateId,
                                @NotNull @Size(max = 100) Map<String, String> answers, @NotBlank String requestId) {
}
