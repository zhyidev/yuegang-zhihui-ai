package com.yuegang.zhihui.user.infrastructure;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * AES-256-GCM 字段加密。IV（初始化向量）是随机生成的，并作为每个加密值的前缀。
 */
public class AddressCipher { // 定义最终类 AddressCipher
    private static final int IV_BYTES = 12; // GCM 模式推荐的 IV 长度为 12 字节
    private final SecretKeySpec key; // 声明 AES 密钥规格对象
    private final int keyVersion; // 声明密钥版本号（用于支持后续密钥轮转）
    private final SecureRandom random; // 声明强随机数生成器

    // 构造函数：接受 Base64 格式的密钥和版本。
    public AddressCipher(String keyBase64, int keyVersion, SecretKeySpec key) {
        this(keyBase64, key, keyVersion, new SecureRandom());
    }

    // 内部构造函数，支持注入随机源（便于测试）
    AddressCipher(String keyBase64, SecretKeySpec key, int keyVersion, SecureRandom random) {
        this.key = key;
        byte[] decoded; // 声明存储解码后的字节数组
        try {
            decoded = Base64.getDecoder().decode(Objects.requireNonNull(keyBase64));
        } // 尝试解码密钥
        catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("PII key must be valid Base64", invalid);
        } // 解码失败抛异常
        if (decoded.length != 32)
            throw new IllegalArgumentException("PII key must contain exactly 32 bytes"); // 校验必须是 256 位密钥
        if (keyVersion < 1 || keyVersion > 65535)
            throw new IllegalArgumentException("PII key version is invalid"); // 初始化 AES 密钥对象
        Arrays.fill(decoded, (byte) 0); // 关键安全动作，立刻擦除内存中的明文密钥数组
        this.keyVersion = keyVersion; // 赋值版本
        this.random = Objects.requireNonNull(random); // 赋值随机源
    }

    public int keyVersion() {
        return keyVersion;
    } // 获取当前加密器使用的密钥版本

    /**
     * 加密方法：传入用户ID、字段名和明文
     *
     * @param userId 用户ID
     * @param field  字段名
     * @param value  明文
     * @return 加密后的字节数组
     */
    public byte[] encrypt(long userId, String field, String value) { // 加密方法：传入用户ID、字段名和明文
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv); // 生成 12 字节随机 IV
        byte[] plain = value.getBytes(StandardCharsets.UTF_8); // 将明文转为 UTF-8 字节
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); // 获取 AES-GCM 实例
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv)); // 初始化为加密模式，标签长度 128 位
            cipher.updateAAD(add(userId, field, keyVersion)); // 注入关联数据（AAD），防止密文在用户间或字段间被挪用
            // 分配缓冲区：长度 = IV长度 + 密文长度（含Tag），并依次存入数组
            return ByteBuffer.allocate(iv.length + cipher.getOutputSize(plain.length)).put(iv).put(cipher.doFinal(plain)).array();
        } catch (GeneralSecurityException failure) {
            throw new IllegalArgumentException("PII encryption failed", failure);
        } // 异常后处理信息值
        finally {Arrays.fill(plain, (byte) 0); //关键安全动作，擦除内存中的明文字节
        }
    }

    public String decrypt(long userId, String field, int storedVersion, byte[] value) { // 解密方法
        // 校验版本号必须匹配，且数据长度必须大于 IV + 认证标签长度
        if (storedVersion != keyVersion || value == null || value.length <= IV_BYTES + 16)
            throw new IllegalArgumentException("PII ciphertext or key version is invalid");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); // 获取实例
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, value, 0, IV_BYTES)); // 使用密文前缀作为IV初始化
            cipher.updateAAD(add(userId, field, storedVersion)); // 注入相同的 AAD 执行完整性校验
            byte[] plain = cipher.doFinal(value, IV_BYTES, value.length - IV_BYTES); // 执行解密
            try { return new String(plain, StandardCharsets.UTF_8); } // 返回解密后的明文
            finally { Arrays.fill(plain, (byte) 0); } // 擦除内存中的明文数据
        } catch (GeneralSecurityException failure) { throw new IllegalStateException("PII decryption failed", failure); } // 解密失败抛异常
    }

    /**
     * 构建 AAD:由用户ID、字段名和版本组成
     *
     * @param userId 用户ID
     * @param field  字段名
     * @param version 版本
     * @return ASCII 字节数组
     */
    private static byte[] add(long userId, String field, int version) { //构建 AAD:由用户ID、字段名和版本组成
        return (userId + ":" + field + ":" + version).getBytes(StandardCharsets.US_ASCII); // 返回 ASCII 字节数组
    }
}