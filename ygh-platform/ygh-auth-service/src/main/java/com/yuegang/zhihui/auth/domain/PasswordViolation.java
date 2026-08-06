package com.yuegang.zhihui.auth.domain;

/**
 * 密码违规项枚举
 */
public enum PasswordViolation { // 各种可能的密码违规原因
    TOO_SHORT, // 长度过短
    TOO_LONG, // 长度过长
    CONTROL_CHARACTER, // 包含非法控制字符
    COMMON_PASSWORD // 属于泄露的常用密码（安全性极差）
}