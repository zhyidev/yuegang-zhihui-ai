package com.yuegang.zhihui.auth.infrastructure; /**
 * 实现固定窗口模式的登录频率限制，同时支持基于凭据（Principal）和基于 IP 的双重限流
 */

import com.yuegang.zhihui.auth.domain.LoginRateLimitDimension;
import com.yuegang.zhihui.auth.domain.LoginRateLimiter;
import com.yuegang.zhihui.auth.domain.SensitiveValueHasher;
import com.yuegang.zhihui.common.redis.RedisKeyBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** 固定窗口限流器，每个维度操作是原子的，且总是同时消耗两个维度的计数 */
public class RedisLoginRateLimiter implements LoginRateLimiter { // 实现接口
    // Lua脚本：执行原子自增并设置过期时间，返回（是否允许，剩余生存时间，当前计数值）
    private static final DefaultRedisScript<List> CONSUME_SCRIPT = new DefaultRedisScript<>("""
        local count = redis.call('INCR', KEYS[1])
        if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[2]) end
        local ttl = redis.call('PTTL', KEYS[1])
        if ttl < 1 then
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            ttl = tonumber(ARGV[2])
        end
        if count > tonumber(ARGV[1]) then return {0, ttl, count} end
        return {1, ttl, count}
        """, List.class);

    private final StringRedisTemplate redis; // Redis 模板
    private final RedisKeyBuilder keys; // key 构建
    private final SensitiveValueHasher hasher; // 敏感值哈希工具（用于脱敏存储IP和用户名）
    private final LoginRateLimitPolicy policy; // 限流策略
    private final String environment; // 环境名

    public RedisLoginRateLimiter(
        StringRedisTemplate redis,
        RedisKeyBuilder keys,
        SensitiveValueHasher hasher,
        LoginRateLimitPolicy policy,
        String environment
    ) {
        this.redis = Objects.requireNonNull(redis, "redis must not be null");
        this.keys = Objects.requireNonNull(keys, "keys must not be null");
        this.hasher = Objects.requireNonNull(hasher, "hasher must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.environment = Objects.requireNonNull(environment, "environment must not be null");
        // 初始化时构建一个演示key，确保构建器逻辑预热
        keys.build(environment, "auth", "login-principal", "validation");
    }

    @Override
    public LoginRateLimitDecision consume(String principal, InetAddress clientIp) { // 核心逻辑
        Objects.requireNonNull(clientIp, "clientIp must not be null");
        // 执行第一个维度：用户凭据限流（哈希后再存入Redis）
        Result principalResult = consumeOne(
            keys.build(environment, "auth", "login-principal", hasher.hashPrincipal(principal)),
            policy.principalLimit(), policy.principalWindow());
        // 执行第二个维度：客户端 IP 限流（哈希后再存储Redis）
        Result ipResult = consumeOne(
            keys.build(environment, "auth", "login-ip", hasher.hashClientAddress(clientIp)),
            policy.ipLimit(), policy.ipWindow());

        // 如果两个维度都允许通过
        if (principalResult.allowed && ipResult.allowed) return LoginRateLimitDecision.allow();

        // 确定哪个维度触发了拒绝
        LoginRateLimitDimension dimension = !principalResult.allowed && !ipResult.allowed
            ? LoginRateLimitDimension.BOTH
            : !principalResult.allowed ? LoginRateLimitDimension.PRINCIPAL : LoginRateLimitDimension.IP; // Lua脚本: GET 获取值，若存在则删除，比较信号是否等于预期，返回 1(成功) 或 0(失败/不存在)

        // 计算重试等待时间（如果两个都限流，选最长的那个 TTL）
        Duration retryAfter = switch (dimension) {
            case PRINCIPAL -> principalResult.retryAfter;
            case IP -> ipResult.retryAfter;
            case BOTH -> principalResult.retryAfter.compareTo(ipResult.retryAfter) >= 0
                ? principalResult.retryAfter : ipResult.retryAfter;
            case NONE -> throw new IllegalStateException("rejected rate-limit decision has no dimension");
        };
// 返回被拒绝的决策
        return new LoginRateLimitDecision(false, dimension, retryAfter);
    }

    private Result consumeOne(String key, int limit, Duration window) { // 执行单个 Redis 维度的计数操作
        List<?> raw = redis.execute(CONSUME_SCRIPT, List.of(key), Integer.toString(limit), Long.toString(window.toMillis())); // 执行 Lua
        if (raw == null || raw.size() != 3 || !(raw.get(0) instanceof Number allowed)
            || !(raw.get(1) instanceof Number ttl)) {
            throw new AccountSecurityPersistenceException("login rate-limit response is invalid", null); // 响应异常处理
        }
        // 返回内部结构：allowed 映射到长整型比较，retryAfter 映射到毫秒时长
        return new Result(allowed.longValue() == 1, Duration.ofMillis(Math.max(1, ttl.longValue())));
    }

    private record Result(boolean allowed, Duration retryAfter) {
    } // 内部临时记录
}
