package com.yuegang.zhihui.common.redis;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/* 为禁止出现在 Redis 键终端敏感值（邮箱）创建确定性的不透明标识符（摘要）*/
public class RedisKeyIdentifier {
    private RedisKeyIdentifier() { // 私有构造函数，防止实例化
    }

    public static String sha256(String value) { // 简单 SHA-256摘要
        requireValue(value); // 校验输入
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        } catch (NoSuchAlgorithmException exception) { //异常处理
            throw new IllegalArgumentException("SHA-256 algorithm not available", exception);

        }
    }
    /**使用环境秘(Pepper)隐藏低熵标识符(如邮箱地址)，防止彩虹表破解*/
    public static String hmacSha256(String value,byte [] pepper) { // 带”盐“的焊锡方法
        requireValue(value); // 校验输入
        if (value == null || pepper.length < 32) { // 强制要求 Pepper 密钥长度
            throw new IllegalArgumentException("Pepper must contain at least 32 bytes"); // 密钥过弱抛出异常
        }
        try{ // 逻辑开始
           Mac mac = Mac.getInstance("hmacSha256"); // 获取 HMAC 实例
            mac.init(new SecretKeySpec(pepper.clone(), "hmacSha256"));// 初始化密钥
            return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); // 执行 HMAC 计算并返回十六进制字符串
        }catch (NoSuchAlgorithmException | InvalidKeyException exception){ //处理异常
            throw new IllegalArgumentException("HMAC-SHA256 algorithm not available", exception); // 抛出非法状态
        }
    }
    private static void requireValue(String value) { // 校验输入值
        if (value == null || value.isEmpty()) { // 非空校验
            throw new IllegalArgumentException("Value must not be null or empty"); // 抛出异常
        }
    }
}
