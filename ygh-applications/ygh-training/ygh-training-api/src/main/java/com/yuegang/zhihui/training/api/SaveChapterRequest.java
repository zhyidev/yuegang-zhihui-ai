package com.yuegang.zhihui.training.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SaveChapterRequest(@NotBlank String courseId, @NotBlank @Size(max = 200) String title,
                                 @Positive int sequenceNo, @Min(0) int minimumActiveSeconds) {
}
