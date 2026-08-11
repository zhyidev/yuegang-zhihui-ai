package com.yuegang.zhihui.auth.domain;

import java.util.Objects;

/**
 * 登录账号模型视图
 */
public record LoginAccount(
        long accountId, // 内部编号 ID
        long userId, // 业务用户 ID
        String accountType, // 账号类型（如 PHONE、EMAIL）
        AccountStatus status, // 账号活跃状态
        PasswordDigest passwordDigest // 密码哈希摘要
) {
    public LoginAccount { // 校验构造函数
        if (accountId <= 0 || userId <= 0) throw new IllegalArgumentException("account identifiers must be positive");
        if (accountType == null || !accountType.matches("[A-Z0-9_]{0,31}")) { // 校验类型安全格式
            throw new IllegalArgumentException("accountType is unsafe");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(passwordDigest, "passwordDigest must not be null");
    }
}
