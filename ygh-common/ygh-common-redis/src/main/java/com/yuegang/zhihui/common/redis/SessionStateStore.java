package com.yuegang.zhihui.common.redis;

import java.time.Instant;

public interface SessionStateStore {
    void register(long accountId, String jwtId, Instant expiresAt, Instant now);
    void rvoke(long accountId, String jwtId, Instant expiresAt, Instant now);
    void disableAccount(long accountId);
    void enableAccount(long accountId);
}
