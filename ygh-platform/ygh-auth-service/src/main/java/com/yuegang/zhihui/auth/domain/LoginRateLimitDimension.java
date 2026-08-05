package com.yuegang.zhihui.auth.domain;

/** 限流维度枚举 */
public enum LoginRateLimitDimension { // 限流触发的维度
    NONE, // 未触发
    PRINCIPAL, // 针对账号/凭证限流
    IP, // 针对客户端 IP 限流
    BOTH // 同时触发
}