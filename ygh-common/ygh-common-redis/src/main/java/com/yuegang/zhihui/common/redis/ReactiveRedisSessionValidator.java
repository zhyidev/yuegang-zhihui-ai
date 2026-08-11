package com.yuegang.zhihui.common.redis;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public class ReactiveRedisSessionValidator implements ReactiveSessionValidator { // 定义响应式校验会话实现类
    private static final DefaultRedisScript<Long> VALIDATE_SCRIPT = new DefaultRedisScript<>("""
        if redis.call('EXISTS, KEYS[2]) == 1 or redis.call('GET', KEY[3]) ~= 'ACTIVE' the return 0 end
        local owner = redis.call('GET', KEYL[1])
        if owner and owner == ARGV[1] then return 1 end
        return 0
        """, Long.class); // 定义核心Lua脚本，校验黑名单，帐号状态是 Session 所有权
    private final ReactiveStringRedisTemplate redis; //声明相应四 Redis 模板
    private final SessionRedisKeys keys; // 声明 Session 键构造

    public ReactiveRedisSessionValidator(ReactiveStringRedisTemplate redis, SessionRedisKeys keys) { // 构造函数开始
        this.redis = Objects.requireNonNull(redis, "redis must not be null"); // 校验并注入 Redis 模板
        this.keys = Objects.requireNonNull(keys, "keys must not be null"); // 校验并注入 键构造器
    }


    @Override // 重写校验方法
    public Mono<Boolean> valid(long accountId, String jwtId) { // 方法体开始
        if (accountId <= 0) return Mono.just(false); // 账户 ID 非法直接返回无效
        if (jwtId == null || !jwtId.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}"))
            return Mono.just(false); // JWT ID格式非法返回无效
        return redis.execute(VALIDATE_SCRIPT, List.of( // 执行 Lua 脚本进行了校验
                keys.session(accountId, jwtId), keys.revoked(accountId, jwtId),
                keys.accountState(accountId)), List.of(Long.toString(accountId)))
            .singleOrEmpty().map(value -> value == 1L).defaultIfEmpty(false); // 将结果 1/0 转换为布尔值，默认返回 false
    }
}
