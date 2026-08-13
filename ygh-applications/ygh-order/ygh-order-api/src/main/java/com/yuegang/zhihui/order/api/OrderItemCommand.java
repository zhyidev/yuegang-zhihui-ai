package com.yuegang.zhihui.order.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderItemCommand(@NotBlank String skuId, @NotBlank String skuCode, @NotBlank String productName,
                               @NotNull @DecimalMin("0.00") BigDecimal unitPrice, @Positive long quantity) {
}
