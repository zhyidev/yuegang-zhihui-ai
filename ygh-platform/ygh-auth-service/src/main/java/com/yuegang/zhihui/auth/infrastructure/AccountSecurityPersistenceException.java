package com.yuegang.zhihui.auth.infrastructure;

/**
 * 账号安全持久化异常类
 */
public final class AccountSecurityPersistenceException extends RuntimeException { // 定义最终类，继承自运行时异常
    public AccountSecurityPersistenceException(String message, Throwable cause) { // 构造函数：接收异常信息和触发原因
        super(message, cause); // 调用父类构造函数
    }
}