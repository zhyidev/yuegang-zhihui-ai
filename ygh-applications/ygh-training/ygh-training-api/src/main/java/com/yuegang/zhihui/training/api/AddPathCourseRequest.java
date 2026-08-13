package com.yuegang.zhihui.training.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AddPathCourseRequest(@NotBlank String courseId, @Positive int sequenceNo, String prerequisiteCourseId) {
}
