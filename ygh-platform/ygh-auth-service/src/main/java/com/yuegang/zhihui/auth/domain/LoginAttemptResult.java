package com.yuegang.zhihui.auth.domain;

/**
 * 登录结果枚举
 */
public enum LoginAttemptResult { // 登录尝试的所有可能结果
    SUCCESS, // 登录成功
    INVALID_CREDENTIALS, // 凭证无效（密码错）
    ACCOUNT_LOCKED, // 账号已锁定
    ACCOUNT_DISABLED, // 账号已禁用
    RATE_LIMITED, // 触发频率限制
    SYSTEM_ERROR // 系统内部错误
}