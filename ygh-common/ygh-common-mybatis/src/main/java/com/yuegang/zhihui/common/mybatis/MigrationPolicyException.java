package com.yuegang.zhihui.common.mybatis;

import java.util.Objects;

/** Fail-fast migration policy error that never includes database credentials. */
public final class MigrationPolicyException extends RuntimeException {
    private final MigrationViolationCode code; // 违规代码

    public MigrationPolicyException(MigrationViolationCode code, String message) {
        super(message); // 传递消息给父类（不是继承？）
        this.code = Objects.requireNonNull(code, "code must not be null"); // 强制非空代码
    }

    public MigrationViolationCode getCode() { // 获取代码
        return code;
    }
}
