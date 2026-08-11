package com.yuegang.zhihui.training.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveQuestionRequest(@NotBlank String gateId, @NotBlank String type,
                                  @NotBlank @Size(max = 5000) String stem, List<String> options,
                                  @NotBlank String correctAnswer, @Size(max = 5000) String explanation,
                                  @Positive int score) {
}
