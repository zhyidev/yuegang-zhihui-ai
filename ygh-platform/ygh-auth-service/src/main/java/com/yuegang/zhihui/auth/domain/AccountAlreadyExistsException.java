package com.yuegang.zhihui.auth.domain;

/** 账号已存在异常 */
public final class AccountAlreadyExistsException extends RuntimeException { // 定义账号已存在运行时异常
    public AccountAlreadyExistsException(Throwable cause) { super("account already exists", cause); } // 构造函数并存入诱因
}