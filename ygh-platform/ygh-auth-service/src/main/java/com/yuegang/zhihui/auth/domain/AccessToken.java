package com.yuegang.zhihui.auth.domain;

import java.time.Instant;
import java.util.Objects;

/** 访问令牌服务*/
public record AccessToken(String value, String jwtId, Instant expireAt) { // 定义访问令牌 Record，包含令牌值、JWT、ID 和过期时间

    public AccessToken { // 紧凑型构造函数，用于校验数据合法性
        if (Objects.requireNonNull(value, "value must not be null").isBlank()) { // 校验令牌值不能为空且不能为空白字符串
            throw new IllegalArgumentException("value must not be blank"); // 抛出参数非法异常
        }
        if (!Objects.requireNonNull(jwtId, "jwtId must not be null") // 校验 JWT ID 不能为空
                .matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")) { // 使用正则校验 JWT ID 是否符合安全规范（字母数字开头，允许特定符号，长度不超过12
            throw new IllegalArgumentException("jwtId is unsafe"); // 抛出 ID 不安全异常
        }
    }

    @Override
    public String toString() { // 重写 toString 方法
        // 为了安全起见，在日志中脱敏显示令牌值和 JWT ID
        return "AccessToken[value=[REDACTED], jwtId=[REDACTED], expiresAt=" + expireAt +"]";
    }
}