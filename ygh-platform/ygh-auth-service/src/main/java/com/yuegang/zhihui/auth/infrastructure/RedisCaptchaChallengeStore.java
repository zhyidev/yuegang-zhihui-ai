package com.yuegang.zhihui.auth.infrastructure;

import com.yuegang.zhihui.auth.domain.CaptchaChallengeStore;
import com.yuegang.zhihui.common.redis.RedisKeyBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** 使用 Redis 存储验证码挑战信息，采用 Lua 脚本保证"验证并删除"的原子性 */
public final class RedisCaptchaChallengeStore implements CaptchaChallengeStore { // 验证码存储
    // Lua脚本: GET 获取值，若存在则删除，比较信号是否等于预期，返回 1(成功) 或 0(失败/不存在)
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
    local actual = redis.call('GET', KEYS[1])
    if not actual then return 0 end
    redis.call('DEL', KEYS[1])
    if actual == ARGV[1] then return 1 end
    return 0
    """, Long.class);
    private final StringRedisTemplate redis; // Redis操作对象
    private final RedisKeyBuilder keys; // Key构造器
    private final String environment; // 调用环境节点对象

    public RedisCaptchaChallengeStore(StringRedisTemplate redis, RedisKeyBuilder keys, String environment) { // 构造注入
        this.redis = Objects.requireNonNull(redis, "redis must not be null");
        this.keys = Objects.requireNonNull(keys, "keys must not be null");
        this.environment = Objects.requireNonNull(environment, "environment must not be null");
    }

    @Override
    public void save(String challengeId, String answerHash, Duration ttl) { // 保存验证码
        if (ttl == null || ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofMinutes(10)) > 0) {
            throw new IllegalArgumentException("captcha ttl must not be between 1ms and 10 minutes"); // 限制有效期10分钟内
        }
        // 使用 SET NX (setIfAbsent) 方式存储，防止 ID 冲突
        Boolean created = redis.opsForValue().setIfAbsent(key(challengeId), answerHash, ttl);
        if (!Boolean.TRUE.equals(created)) throw new IllegalStateException("captcha challenge id collision"); // ID重复则报错
    }


    @Override
    public boolean consume(String challengeId, String presentedAnswerHash) { // 验证并销毁
        // 执行 Lua 脚本实现单次有效消费，防止通过重试暴力破解同一验证码
        return Long.valueOf(1L).equals(redis.execute(CONSUME, List.of(challengeId), presentedAnswerHash));
    }

    private String key(String challengeId) {
        return keys.build(environment, "auth", "captcha", challengeId); // 生成格式化的 Redis Key
    }
}
