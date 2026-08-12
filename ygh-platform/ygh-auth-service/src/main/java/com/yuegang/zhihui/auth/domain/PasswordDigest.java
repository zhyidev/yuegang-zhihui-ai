package com.yuegang.zhihui.auth.domain;

import java.util.Objects;

/**
 * 密码摘要
 */
public record PasswordDigest(String hash, String algorithm, int version) { // 包含哈希串，算法名称和版本的摘要 Record

    public PasswordDigest { // 校验
        Objects.requireNonNull(hash, "hash must not be null");
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        if (hash.isBlank() || algorithm.isBlank() || version < 1) { // 关键元数据不能为空
            throw new IllegalArgumentException("password digest metadata is invalid");
        }
    }

    @Override
    public String toString() { // 脱敏 toString
        return "PasswordDigest[algorithm=" + algorithm + ", version=" + version + ", hash=[REDACTED]]";
    }

}
