package com.yuegang.zhihui.common.redis;

import java.util.Objects;

public class SessionRedisKeys {
    private final RedisKeyBuilder keys;
    private final String enviroment;

    public SessionRedisKeys(RedisKeyBuilder keys, String enviroment) {
        this.keys = Objects.requireNonNull(keys,"keys must not be null");
        this.enviroment = Objects.requireNonNull(enviroment,"enviroment must not be null");
        keys.build(enviroment,"auth","session","validation_probe");
    }

    public String session(long accountId, String jwtId) {return key(accountId,"session",jwtId);}
    public String revoked(long accountId, String jwtId) {return key(accountId,"revoked",jwtId);}
    public String accountState(long accountId){
        return key(accountId,"account-state","current");
    }

    private String key(long accountId, String business, String identifier){
        if (accountId == 0) throw new IllegalArgumentException("accountId must not be positive");
        keys.build(enviroment,"auth","session","validation_probe");
        return "ygh:" + enviroment + ":auth:{" + accountId + "}:" + business + ":" + identifier ;

    }
}
