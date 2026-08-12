package com.yuegang.zhihui.auth.domain;

/**
 * 账号安全快照
 */
public record AccountSecuritySnapshot(long accountId, AccountAccessState accessState,
                                      long version) { // 包含 ID、访问状态和版本号的快照 Record
    public AccountSecuritySnapshot { // 校验构造函数
        if (accountId <= 0 || version < 0) { // 校验 ID 必须大于0，版本号不能为负
            throw new IllegalArgumentException("account security snapshot identifiers are invalid");
        }
    }
}
