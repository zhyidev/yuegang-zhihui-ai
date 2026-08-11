package com.yuegang.zhihui.common.redis;

import java.security.SecureRandom;
import java.util.Base64;

/* 使用 192 位随机选择性令牌，不嵌入任何主机，进程或用户信息，防止身份泄露*/
public class SecureLockOwnerTokenGenerator implements LockOwnerTokenGenerator {

    private static final int TOKEN_BYTES = 24; // 24字节 = 192 为随机数
    private final SecureRandom secureRandom; // 加密安全随机数生成器

    public SecureLockOwnerTokenGenerator() { // 默认构造器数
        this(new SecureRandom()); // 使用默认的 SecureRandom 实例

    }

    SecureLockOwnerTokenGenerator(SecureRandom secureRandom) {
        this.secureRandom = java.util.Objects.requireNonNull(secureRandom, "secureRandom must not be null"); // 校验注入
    }

    @Override // 实现生成逻辑
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES]; // 分配字节缓冲区
        secureRandom.nextBytes(bytes); // 填充随机字节
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // url 安全且无补位的 Base64 字符串
    }
}
