package com.yuegang.zhihui.auth.domain;

@FunctionalInterface
public interface LoginAttemptRepository { // 登录尝试记录存储接口
    void save(LoginAttempt attempt); // 保存登录尝试审计记录
}
