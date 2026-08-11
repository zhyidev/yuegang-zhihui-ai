package com.yuegang.zhihui.auth.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * 账号锁定策略
 */
public final class AccountLockPolicy { // 账号锁定策略逻辑类
    private final int maximumFailures; // 允许的最大失败次数
    private final Duration lockDuration; // 达到上限后的锁定持续时长

    public AccountLockPolicy(int maximumFailures, Duration lockDuration) { // 构造函数
        if (maximumFailures < 1) { // 校验最大失败次数必须为正数
            throw new IllegalArgumentException("maximumFailures must be positive");
        }
        this.lockDuration = Objects.requireNonNull(lockDuration, "lockDuration must not be null"); // 持续时长不能为空
        if (lockDuration.isZero() || lockDuration.isNegative()) { // 时长不能为0或负数
            throw new IllegalArgumentException("lockDuration must be positive");
        }
        this.maximumFailures = maximumFailures; // 赋值
    }

    public boolean authenticationAllowed(AccountAccessState state, Instant now) { // 判断当前状态是否允许进行身份验证
        Objects.requireNonNull(state, "state must not be null");
        return state.status() == AccountStatus.ACTIVE && !state.lockedAt(now);
    }

    public AccountAccessState recordFailure(AccountAccessState state, Instant now) { // 记录一次失败尝试并返回新状态
        Objects.requireNonNull(state, "state must not be null"); // 状态检查
        Objects.requireNonNull(now, "now must not be null"); // 时间检查
        if (state.status() != AccountStatus.ACTIVE || state.lockedAt(now)) { // 如果账号本就不活跃或已锁定，则直接返回原状态
            return state;
        }
        // 如果之前是从锁定中恢复的（lockedUntil有值但已过期），重置计数，否则累加失败次数
        int priorFailures = state.lockedUntil().isPresent() ? 0 : state.failedLoginCount();
        int failures = Math.min(maximumFailures, priorFailures + 1); // 累加计数，不超过最大次数
        // 如果达到最大次数，则计算截止时间
        Optional<Instant> lockedUntil = failures >= maximumFailures
            ? Optional.of(now.plus(lockDuration)) : Optional.empty();
        return new AccountAccessState(state.status(), failures, lockedUntil); // 返回更新后的状态
    }

    public AccountAccessState recordSuccess(AccountAccessState state, Instant now) { // 记录一次成功登录
        Objects.requireNonNull(state, "state must not be null"); // 状态检查
        Objects.requireNonNull(now, "now must not be null"); // 时间检查
        if (state.status() != AccountStatus.ACTIVE || state.lockedAt(now)) { // 若已锁定或不活跃则状态不变
            return state;
        }
        return AccountAccessState.active(); // 登录成功，重置为初始活跃状态（清空失败计数和锁定时间）
    }

}
