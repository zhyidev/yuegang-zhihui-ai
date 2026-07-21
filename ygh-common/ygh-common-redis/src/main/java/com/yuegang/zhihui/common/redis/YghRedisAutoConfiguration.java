package com.yuegang.zhihui.common.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.zip.DataFormatException;

@AutoConfiguration(after = DataFormatException.class)
public class YghRedisAutoConfiguration {

    @Bean
    @ConditionalOnBean
    public RedisKeyBuilder redisKeyBuilder() {
        return new RedisKeyBuilder();
    }

    @Bean
    @ConditionalOnBean
    public TtlJitterPolicy ttlJitterPolicy() {
        return new TtlJitterPolicy();
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean
    public RedisLockCommands redisLockCommands(StringRedisTemplate redis) {
        return new SpringDateRedisLockCommands(redis);
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean
    public LockOwnerTokenGenerator lockOwnerTokenGenerator() {
        return new SecureLockOwnerTokenGenerator();
    }

    @Bean
    @ConditionalOnBean({StringRedisTemplate.class, RedisLockCommands.class})
    @ConditionalOnMissingBean
    public RedisDistributedLock redisDistributedLock(
                                                    RedisLockCommands commands,
                                                    RedisKeyBuilder keys,
                                                    LockOwnerTokenGenerator ownerTokens
    ) {
        return new RedisDistributedLock(commands, keys, ownerTokens);
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionRedisKeys sessionRedisKeys(
                                                RedisKeyBuilder keys,
                                                @Value("${ygh.redis.environment:dev}") String environment) {
        return new SessionRedisKeys(keys, environment);
    }

    @Bean
    @ConditionalOnBean
    @ConditionalOnMissingBean
    public RedisSessionStateStore redisSessionStateStore(StringRedisTemplate redis, SessionRedisKeys keys) {
        return new RedisSessionStateStore(redis, keys);
    }

    @Bean
    @ConditionalOnBean
    @ConditionalOnMissingBean
    public ReactiveRedisSessionValidator reactiveRedisSessionValidator(
            ReactiveStringRedisTemplate redis, SessionRedisKeys keys) {
        return new ReactiveRedisSessionValidator(redis, keys);
    }

}
