package com.yuegang.zhihui.search.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SwitchIndexRequest(@NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9._-]{1,63}") String version,
                                 @AssertTrue boolean confirm) {
}
