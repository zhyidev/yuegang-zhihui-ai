package com.yuegang.zhihui.auth.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * 频率限制决策
 */
public record LoginRateLimitDecision( // 登录频率限制决策结果 Record
                                      boolean allowed, // 是否允许继续
                                      LoginRateLimitDimension rejectedDimension, // 被拒绝的维度（如 IP 或 账号）
                                      Duration retryAfter // 建议重试的等待时长
) {
    public LoginRateLimitDecision { // 校验决策逻辑一致性
        Objects.requireNonNull(rejectedDimension, "rejectedDimension must not be null");
        Objects.requireNonNull(retryAfter, "retryAfter must not be null");
        // 校验状态矛盾的情况：例如允许通过但却有等待时长，或者不允许通过但等待时长为0
        if (retryAfter.isNegative() || (allowed && !retryAfter.isZero())
                || (allowed && rejectedDimension != LoginRateLimitDimension.NONE)
                || (!allowed && (retryAfter.isZero() || rejectedDimension == LoginRateLimitDimension.NONE))) {
            throw new IllegalArgumentException("rate-limit decision is inconsistent");
        }
    }

    public static LoginRateLimitDecision allow() { // 静态工厂：返回允许通过的决策
        return new LoginRateLimitDecision(true, LoginRateLimitDimension.NONE, Duration.ZERO);
    }
}