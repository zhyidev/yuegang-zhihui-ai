package com.yuegang.zhihui.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * 账号访问状态
 */
public record AccountAccessState( // 定义账号访问状态
                                  AccountStatus status, // 账号状态（如激活、禁用）
                                  int failedLoginCount, // 登录失败次数
                                  Optional<Instant> lockedUntil) { // 锁定截止时间（可选）

    public AccountAccessState { // 校验构造函数
        Objects.requireNonNull(status, "status must not be null"); // 状态不能为空
        Objects.requireNonNull(lockedUntil, "lockedUntil must not be null"); // 锁定容器
        if (failedLoginCount < 0) { // 失败次数不能为负数
            throw new IllegalArgumentException("failedLoginCount must not be negative");
        }
    }

    public static AccountAccessState active() { // 静态工厂方法：创建一个初始激活状态
        return new AccountAccessState(AccountStatus.ACTIVE,  0,  Optional.empty());
    }

    public boolean lockedAt(Instant now) { // 判断在给定时间账号是否处于锁定状态
        Objects.requireNonNull(now, "now must not be null"); // 时间点不能为空
        // 如果存在锁定截止时间，且该时间在当前时间之后，则表示已锁定
        return lockedUntil.filter(  until -> until.isAfter(now)).isPresent();
    }

}