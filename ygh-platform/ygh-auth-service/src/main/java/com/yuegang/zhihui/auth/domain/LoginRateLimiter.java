package com.yuegang.zhihui.auth.domain;

import java.net.InetAddress;

/**
 * 登录限流器接口
 */
@FunctionalInterface
public interface LoginRateLimiter { // 登录限流器接口
    // 根据登录凭证和客户端 IP 消费一个令牌，并返回决策结果
    LoginRateLimitDecision consume(String principal, InetAddress clientIP);
}
