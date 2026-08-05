package com.yuegang.zhihui.auth.domain;

import java.util.Set;

/** 密码校验结果类 */
public record PasswordValidationResult(Set<PasswordViolation> violations) { // 存储一组密码违规项的结果 Record

    public PasswordValidationResult {
        violations = Set.copyOf(violations); // 创建不可变副本
    }

    public boolean valid() { // 判断是否完全符合策略
        return violations.isEmpty();
    }

    @Override
    public String toString() {
        return "PasswordValidationResult[violations=" + violations + ']';
    }
}