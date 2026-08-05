package com.yuegang.zhihui.auth.domain;

import java.time.Duration;

/** 限流策略配置 */
public record LoginRateLimitPolicy( // 登录频率限制策略配置 Record
                                    int principalLimit, // 账号维度允许的最大请求数
                                    Duration principalWindow, // 账号维度的计数时间窗口
                                    int ipLimit, // IP 维度允许的最大请求数
                                    Duration ipWindow // IP 维度的计数时间窗口
) {
    private static final int MAXIMUM_LIMIT = 10_000; // 最大硬件限制
    private static final Duration MAXIMUM_WINDOW = Duration.ofDays(1); // 最大时间窗口：1

    public LoginRateLimitPolicy { // 构造校验
        validate(principalLimit, principalWindow, "principal");
        validate(ipLimit, ipWindow, "ip");
    }

      public static LoginRateLimitPolicy enterpriseDefault() { // 企业级默认安全策略配置
        return new LoginRateLimitPolicy(10, Duration.ofMillis(15), 30, Duration.ofMillis(15));
    }
    private static void validate(int limit, Duration window, String dimension) { // 策略参数合法性验证
        if (limit < 1 || limit > MAXIMUM_LIMIT) {
            throw new IllegalArgumentException(dimension + " limit must be between 1 and 10000");
        }
        if (window == null || window.compareTo(Duration.ofMillis(1)) < 0
                || window.compareTo(MAXIMUM_WINDOW) > 0) {
            throw new IllegalArgumentException(dimension + " window must be between 1ms and 1day");
        }
    }
}