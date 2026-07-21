package com.yuegang.zhihui.common.redis;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class RedisKeyIdentifier {

    private RedisKeyIdentifier() {
    }

    public static String sha256(String value) {
        requireValue(value);
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalArgumentException("SHA-256 is unavailable", exception);
        }
    }

    public static String hmacSha256(String value, byte[] pepper) { // 改名 pepper
        requireValue(value);
        if (pepper == null || pepper.length < 32) {
            throw new IllegalArgumentException("pepper must contain at least 32 bytes");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            // 使用 SecretKeySpec 包装字节数组作为密钥
            SecretKeySpec keySpec = new SecretKeySpec(pepper, "HmacSHA256");
            mac.init(keySpec);
            return java.util.HexFormat.of().formatHex(
                    mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalArgumentException("HmacSHA256 is unavailable", exception);
        } catch (java.security.InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid key for HMAC", e);
        }
    }

    private static void requireValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be null or blank");
        }
    }
}