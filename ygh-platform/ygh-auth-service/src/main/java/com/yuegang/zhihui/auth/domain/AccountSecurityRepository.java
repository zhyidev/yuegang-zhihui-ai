package com.yuegang.zhihui.auth.domain;

import java.util.Optional;

/**
 * 账号安全仓储接口
 */
public interface AccountSecurityRepository { // 账号安全相关数据持久化接口
    Optional<AccountSecuritySnapshot> findById(long accountId); // 根据账号 ID 查找安全快照

    // 使用原子操作（Compare-And-Set）更新访问状态，防止并发冲突
    boolean compareAndSetAccessState(long accountId, long expectedVersion, AccountAccessState newState);
}
