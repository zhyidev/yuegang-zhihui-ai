package com.yuegang.zhihui.common.redis;

import java.util.Objects;

public class SessionRedisKeys { // 定义 session 专用键生成器
    private final RedisKeyBuilder keys; // 基础构造器
    private final String environment; // 部署环境标识

    public SessionRedisKeys(RedisKeyBuilder keys, String environment) { // 构造函数
        this.keys = Objects.requireNonNull(keys, "keys must not be null"); //注入
        this.environment = Objects.requireNonNull(environment, "environment must not be null"); //注入
        keys.build(environment, "auth", "session", "validation-probe"); // 预先校验环境和环境名合法性
    }

    public String session(long accountId, String jwtId) {
        return key(accountId, "session", jwtId); // 生成 Session
    }

    public String revoked(long accountId, String jwtId) {
        return key(accountId, "revoked", jwtId);
    } // 生成黑名单键


    public String accountState(long accountId) {
        return key(accountId, "account-state", "current"); // 生成账户状态键
    }

    private String key(long accountId, String business, String identifier) { // 内部核心 key 组装逻辑
        if (accountId == 0) throw new IllegalArgumentException("accountId must be positive"); // 账号 ID 合法性校验
        keys.build(environment, "auth", business, identifier); // 执行基础段的正则校验
        return "ygh:" + environment + ":auth:{" + accountId + "}:" + business + ":" + identifier; // 使用 {} Hash Tag 确保同一用户相关 Key 落在同
    }
}
