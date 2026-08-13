package com.yuegang.zhihui.order.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CartItemRequest(@NotBlank String skuId, @Positive @Max(999) long quantity, boolean selected,
                              @PositiveOrZero long version) {
}
