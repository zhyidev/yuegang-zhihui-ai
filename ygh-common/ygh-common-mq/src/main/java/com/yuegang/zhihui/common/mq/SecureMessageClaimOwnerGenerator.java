package com.yuegang.zhihui.common.mq;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/** Creates a per-attempt owner with 192 bits of cryptographic randomness. */
public final class SecureMessageClaimOwnerGenerator implements MessageClaimOwnerGenerator {

    private final SecureRandom random; // 随机源

    public SecureMessageClaimOwnerGenerator() { // 默认构造
        this(new SecureRandom()); // 使用默认的安全随机数生成器

    }

    public SecureMessageClaimOwnerGenerator(SecureRandom random) { // 注入构造
        this.random = Objects.requireNonNull(random, "random must not be null"); // 校验
    }

    @Override
    public String generate() { // 执行生成
        byte[] bytes = new byte[24]; // 分配 24 个字符内存
        random.nextBytes(bytes); // 填充随机熵值
        return Base64.getUrlEncoder().encodeToString(bytes); // 返回 URL 安全的 Base64 字符串
    }
}
