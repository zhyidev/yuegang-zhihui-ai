package com.yuegang.zhihui.system.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SaveDictionaryItemRequest(@NotBlank @Size(max = 64) String key, @NotBlank @Size(max = 500) String value,
                                        int sortOrder, boolean enabled, @PositiveOrZero long version) {
}
