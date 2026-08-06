package com.yuegang.zhihui.auth.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * 刷新令牌对
 */
public record RefreshTokenPair(String value, Instant expiresAt) { // 用于返回给客户端的刷新令牌数据 Record

    public RefreshTokenPair { // 校验
        if (Objects.requireNonNull(value, "value must not be null").isBlank()) {
            throw new IllegalArgumentException("value must not be null");
        }
    }
}