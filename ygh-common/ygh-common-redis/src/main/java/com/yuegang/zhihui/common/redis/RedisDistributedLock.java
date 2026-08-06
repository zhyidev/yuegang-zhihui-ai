package com.yuegang.zhihui.common.redis;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/* 短生命的redis 锁，其释放和持续操作始终原子地比较持有者的令牌*/
public final class RedisDistributedLock { //类定义开始

    public static final Duration MIN_LEASE = Duration.ofSeconds(1); // 定义最小租期时间为1秒
    public static final Duration MAX_LEASE = Duration.ofSeconds(5); // 定义最大租期时间为5秒

    private final RedisLockCommands commands; // 原子命令执行器

    private final RedisKeyBuilder keys;  // 建构造器，用于规范化校验

    private final LockOwnerTokenGenerator ownerTokens; // 令牌生成器

    public RedisDistributedLock(RedisLockCommands commands, // 注入底层命令
                                RedisKeyBuilder keys, // 注入键构造器
                                LockOwnerTokenGenerator ownerTokens // 注入令牌生成逻辑
    ) {
        this.commands = Objects.requireNonNull(commands, "commands must not be null"); // 校验并赋值
        this.keys = Objects.requireNonNull(keys, "keys must not be null"); // 校验并赋值
        this.ownerTokens = Objects.requireNonNull(ownerTokens, "ownerTokens must not be null"); // 校验并赋值

    }

    static void requireLease(Duration lease) { // 金泰工具：租约范围校验
        Objects.requireNonNull(lease, "lease must not be null"); // 不能为空
        if (lease.compareTo(MIN_LEASE) < 0 || lease.compareTo(MAX_LEASE) > 0) {
            throw new IllegalArgumentException("lease must be between 1 second and 5 minutes"); //不合法抛出异常
        }
    }

    public Optional<RedisLockHandle> tryAcquire(String key, Duration lease) { //尝试获取锁定方法
        requireCanonicalKey(key); // 检验 key 是否符合规范
        requireLease(lease); // 检验租约是否允许的
        String owner = ownerTokens.generate(); // 生成本次请求唯一持有者标识
        var handle = new RedisLockHandle(key, owner, lease); // 构造锁句柄
        return commands.setIfAbsent(key, owner, lease) ? Optional.of(handle) : Optional.empty(); // 执行

    }

    public boolean release(RedisLockHandle handle) { // 释放锁的方法
        Objects.requireNonNull(handle, "handle must not be null"); // 句柄不能为空
        requireCanonicalKey(handle.key()); // 校验 key 规范
        return commands.releaseIfOwner(handle.key(), handle.owner());// 仅当value 匹配持有者才进行删除，防止误删他人的锁
    }

    public boolean renew(RedisLockHandle handle, Duration lease) {
        Objects.requireNonNull(handle, "handle must not be null"); // 句柄不能为空
        requireCanonicalKey(handle.key());
        requireLease(lease);
        return commands.renewIfOwner(handle.key(), handle.owner(), lease);
    }

    private void requireCanonicalKey(String key) { // 私有方法：强制要求 key 使用系统规范的格式
        if (!keys.isCanonical(key)) { // 如果不符合ygh:env:service..格式
            throw new IllegalArgumentException("lock key must the canonical ygh namespace");// 抛出安全限制异常

        }

    }

}
