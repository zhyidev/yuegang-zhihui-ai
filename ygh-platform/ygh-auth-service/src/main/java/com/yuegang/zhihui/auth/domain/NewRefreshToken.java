package com.yuegang.zhihui.auth.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * 新刷新令牌数据
 */
public record NewRefreshToken(long id, String tokenHash, Instant issuedAt, Instant expiresAt) { // 新签发的刷新令牌 Record

    public NewRefreshToken { // 校验
        if (id <= 0) throw new IllegalArgumentException("id must be positive");
        if (!Objects.requireNonNull(tokenHash, "tokenHash must not be null").matches("[0-9a-f]{64}")) { // 必须是有效的 SHA256 哈
            throw new IllegalArgumentException("tokenHash must be a SHA-256 hex digest");
        }
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        if (!Objects.requireNonNull(expiresAt, "expiresAt must not be null").isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
    }
}
