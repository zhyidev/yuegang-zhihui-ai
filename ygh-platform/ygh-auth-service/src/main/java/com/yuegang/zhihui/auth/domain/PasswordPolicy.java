package com.yuegang.zhihui.auth.domain;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 密码强度策略
 */
public class PasswordPolicy { // 密码复杂性校验策略类
    private static final int MIN_LENGTH = 15; // 系统推荐最小长度
    private static final int MAX_LENGTH = 128; // 最大长度

    private final int minimumLength; // 配置的最小长度
    private final int maximumLength; // 配置的最大长度
    private final CompromisedPasswordChecker compromisedPasswordChecker; // 泄露库检查器

    public PasswordPolicy(int minimumLength, int maximumLength, CompromisedPasswordChecker compromisedPasswordChecker) {
        if (minimumLength < 1 || maximumLength < minimumLength) {
            throw new IllegalArgumentException("password length bounds are invalid");
        }
        this.minimumLength = minimumLength;
        this.maximumLength = maximumLength;
        this.compromisedPasswordChecker = Objects.requireNonNull(compromisedPasswordChecker, "compromisedPasswordChecker must not be null");
    }

    public PasswordValidationResult validate(char[] password) { // 核心：执行密码校验 n
        if (password == null) {
            return new PasswordValidationResult(Set.of(PasswordViolation.TOO_SHORT));
        }
        var violations = EnumSet.noneOf(PasswordViolation.class); // 收集违规项
        int length = Character.codePointCount(password, 0, password.length); // 准确计算
        if (length < minimumLength) {
            violations.add(PasswordViolation.TOO_SHORT);
        }
        if (length > maximumLength) {
            violations.add(PasswordViolation.TOO_LONG);
        }
        for (char character : password) {
            if (Character.isISOControl(character)) { // 禁止密码中出现控制字符
                violations.add(PasswordViolation.CONTROL_CHARACTER);
                break;
            }
        }
        if (compromisedPasswordChecker.isCompromised(password)) { // 检查是否属于弱密码/已泄露密码
            violations.add(PasswordViolation.COMMON_PASSWORD);
        }

        return new PasswordValidationResult(violations); // 返回校验结果集
    }

}
