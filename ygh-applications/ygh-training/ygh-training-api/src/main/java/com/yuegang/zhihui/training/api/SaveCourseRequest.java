package com.yuegang.zhihui.training.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveCourseRequest(@NotBlank @Size(max = 200) String title, @Size(max = 5000) String description,
                                @Min(0) int estimatedMinutes, @Min(0) @Max(100) int passScore) {
}
