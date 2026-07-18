package com.yuegang.zhihui.common.core;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public record Money(
                     @JsonFormat(shape = JsonFormat.Shape.STRING)BigDecimal amount,
                     CurrencyCode currency
        ) {
    public static final int SCALE = 2; // 统一两位小数

    public Money{
        if (amount == null) {
            throw new IllegalArgumentException("amount cannot be null");
        }
        if (amount.scale() != SCALE) {
            throw new IllegalArgumentException("amount scale must be exactly " + SCALE);
        }
        if (amount.signum() < 0){
            throw new IllegalArgumentException("amount cannot be negative");
        }
        if (currency == null) {
            throw new IllegalArgumentException("currencyCode cannot be null");
        }
    }
}
