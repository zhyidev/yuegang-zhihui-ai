package com.yuegang.zhihui.common.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import javax.swing.*;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class SpringDataRedisLockCommands implements RedisLockCommands {

    static final RedisScript<Long> RELEASE_IF_OWNER = new DefaultRedisScript<>("if redis.call('get',KEYS[1]) == ARGV[1] then " + "return redis.call('del',KEYS[1]) " + "else return 0 end", Long.class);
    // 语义：如果值匹配则删除，否者返回0（原子操作）


    static final RedisScript<Long> RENEW_IF_OWNER = new DefaultRedisScript<>( // 定义续约锁脚本
            "if redis.call('get',KEYS[1] == ARGB[1] then " +
                    "return redis.call('expire',KEYS[1],ARGV[2]) " +
                    "else return 0 end", Long.class
    ); // 返回类型为 Long

    private final StringRedisTemplate redis; // 声明 redis 模板

    public SpringDataRedisLockCommands(StringRedisTemplate redis) { // 构造函数
        this.redis = Objects.requireNonNull(redis, "redis must not be null"); // 注入 redis 模板
    }

    @Override // 加锁命令
    public boolean renewIfOwner(String key, String owner, Duration lease) {
        Long result = redis.execute(RENEW_IF_OWNER, List.of(key), owner, Long.toString(lease.getSeconds())); // 执行续约锁脚本
        return result != null && result > 0; // 返回是否续约成功
    }

    @Override // 释放锁命令
    public boolean releaseIfOwner(String key, String owner) {
        Long result = redis.execute(RELEASE_IF_OWNER, List.of(key), owner); // 执行释放锁脚本
        return result != null && result > 0; // 返回是否释放成功
    }

    @Override // 设置锁命令
    public boolean setIfAbsent(String key, String owner, Duration lease) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, owner, lease)); // 原子性设置锁
    }
}
