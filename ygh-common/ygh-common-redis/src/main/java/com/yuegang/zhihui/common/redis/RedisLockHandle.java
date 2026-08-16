package com.yuegang.zhihui.common.redis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/* 成功获得锁后返回持有全凭证。 */
public record RedisLockHandle(
        String key,
        @JsonIgnore String owner,
        Duration lease) { // 定义 Record ， 忽略 JSON 序列化终端 owner 令牌
    private static final Pattern OWNER =
            Pattern.compile("[A-Za-z0-9_-]{32,128}"); // 持有者令牌正则，允许32-128位的安全字符

    public RedisLockHandle { // 紧凑构造函数进行属性校验
        Objects.requireNonNull(key, "key must not be null"); // key 不能为空
        Objects.requireNonNull(owner, "owner must not be null"); // 持有者标识不能为空
        Objects.requireNonNull(lease, "lease must not be null"); // 租约不能为空
        if (key.isBlank()) { // key 不能为空字符
            throw new IllegalArgumentException("key must not be blank"); // 抛出异常
        }
        if (!OWNER.matcher(owner).matches()) { // 持有者令牌格式校验
            throw new IllegalArgumentException("owner token is malformed"); // 格式错误，抛出异常
        }
        //        RedisDistributedLock.

    }

    @Override // 重写toString
    public String toString() {
        return "RedisLockHandle[key=" + key + ", owner=[REDACTED], lease=" + lease + ']';
    }
}
