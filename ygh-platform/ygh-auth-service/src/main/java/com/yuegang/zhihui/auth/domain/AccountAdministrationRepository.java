package com.yuegang.zhihui.auth.domain;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface AccountAdministrationRepository { // 账号行政管理存储接口
    Optional<StatusChange> changeStatus(long userId, AccountStatus status, long expectedVersion,
                                        long operationUserId, String reason);

    // 定义状态变更结果的内部记录类
    record StatusChange(long accountId, long userId, AccountStatus status, long version, OffsetDateTime updatedAt) {
    }
}
