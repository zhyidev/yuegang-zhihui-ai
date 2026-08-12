package com.yuegang.zhihui.common.redis;

import java.time.Instant;

public interface SessionStateStore { // 定义会话声明周期管理契约
    void register(long accountId, String jwtId, Instant expiresAt, Instant now); // 注册一个新生成的合法权益

    void revoke(long accountId, String jwtId, Instant expiresAt, Instant now); // 撤销一个已注册的合法权益

    void disableAccount(long accountId); // 锁定账号，使该用户所有现有 Session 失效

    void enableAccount(long accountId); // 解锁账号
}
