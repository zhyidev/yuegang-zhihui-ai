package com.yuegang.zhihui.auth.domain;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * 生成加盐的，确定性的审计标识符，而无需在审计日志中存储原始敏感数据
 */
public final class SensitiveValueHasher {
    private static final int MINIMUM_PEPPER_BYTES = 32; // 要求 Pepper 最小长度为 32 字节
    private final SecretKeySpec key; // 内部使用 HMAC 秘钥

    public SensitiveValueHasher(byte[] pepper) { // 构造函数
        if (pepper == null || pepper.length < MINIMUM_PEPPER_BYTES) {
            throw new IllegalArgumentException("audit pepper must contain at least 32 bytes");
        }
        // 基于传入的 Pepper 生成 HMAC-SHA256 密钥
        this.key = new SecretKeySpec(Arrays.copyOf(pepper, pepper.length), "HmacSHA256");
    }

    public String hashPrincipal(String principal) { // 对登录凭证进行加盐哈希（审计用）
        String normalized = PrincipalNormalizer.normalize(principal); // 先规范化
        ByteBuffer buffer = null;
        byte[] bytes = null;
        try {
            buffer = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(normalized)); // 转为字节
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return hmac(bytes); // 执行 HMAC
        } catch (CharacterCodingException malformed) {
            throw new IllegalArgumentException("principal is not valid UTF-8", malformed);
        } finally {
            if (buffer != null && buffer.hasArray()) Arrays.fill(buffer.array(), (byte) 0); // 清理敏感内存
            if (bytes != null) Arrays.fill(bytes, (byte) 0);
        }
    }

    /**
     * 对客户端 IP 进行加盐哈希（审计用）
     *
     * @param address 客户端 IP 地址
     * @return 哈希后的字符串
     */
    public String hashClientAddress(InetAddress address) { // 对客户端 IP 进行加盐哈希（审计用）
        if (address == null) throw new IllegalArgumentException("address must not be null");
        byte[] raw = address.getAddress(); // 获取 IP 原始字节
        byte[] domainSeparated = new byte[raw.length + 1]; // 增加或分隔字节，防止跨域碰撞攻击
        domainSeparated[0] = 1; // 域标识：1 代表 IP 地址
        System.arraycopy(raw, 0, domainSeparated, 1, raw.length);
        try {
            return hmac(domainSeparated); // 计算 HMAC
        } finally {
            Arrays.fill(raw, (byte) 0); // 清理内存
            Arrays.fill(domainSeparated, (byte) 0);
        }
    }

    public String hashCaptchaAnswer(String answer) { // 对验证码答案进行加盐哈希（比对用）
        if (answer == null || !answer.matches("[A-Za-z0-9]{6}")) { // 校验格式
            throw new IllegalArgumentException("captcha answer must contain six alphanumeric characters");
        }
        byte[] raw = answer.toUpperCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII); // 统一转大写处理
        byte[] domainSeparated = new byte[raw.length + 1];
        domainSeparated[0] = 2; // 域标识：2 代表验证码
        System.arraycopy(raw, 0, domainSeparated, 1, raw.length);
        try {
            return hmac(domainSeparated);
        } finally {
            Arrays.fill(raw, (byte) 0); // 清理内存
            Arrays.fill(domainSeparated, (byte) 0);
        }
    }

    private String hmac(byte[] value) { // 底层通用 HMAC 执行逻辑
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); // 实例化 HMAC-SHA256
            mac.init(key); // 使用内部密钥初始化
            return HexFormat.of().formatHex(mac.doFinal(value)); // 计算并格式化为十六进制字符串
        } catch (GeneralSecurityException impossible) {
            throw new IllegalStateException("HmacSHA256 unavailable", impossible);
        }
    }

    @Override
    public String toString() { // 脱敏 toString
        return "SensitiveValueHasher[pepper=[REJECTED]]";
    }
}