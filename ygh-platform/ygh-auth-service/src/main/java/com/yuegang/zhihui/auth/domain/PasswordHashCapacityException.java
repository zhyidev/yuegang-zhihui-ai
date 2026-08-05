package com.yuegang.zhihui.auth.domain;

/** 哈希容量异常 */
public class PasswordHashCapacityException extends RuntimeException { // 并发哈希任务达到上限时的异常类

    public PasswordHashCapacityException(String message) {
        super(message);
    }

    public PasswordHashCapacityException(String message, Throwable cause) {
        super(message, cause);
    }

}