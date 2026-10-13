package com.yuegang.zhihui.system.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// 该类用于对系统敏感信息（如AI 供应商的 API Key）进行AES-GCM高强度加密存储。
public class SystemSecretCipher { // 定义最终类，系统密钥加密器
    // 定义关联数据（AAD），用于增强 GCM 模式的校验可靠性，防止密文被转移到其他上下文
    private static final byte[] AAD =
            "ygh:system:ai-provider:api-key:v1".getBytes(StandardCharsets.UTF_8);
    private final SecretKeySpec key; // 声明 AES 密钥对象
    private final SecureRandom random = new SecureRandom(); // 实例化安全随机数生成器，用于产生 Nonce

    public SystemSecretCipher(byte[] masterKey) { // 构造函数：传入 32 字节（256位）的主密钥
        if (masterKey == null || masterKey.length != 32) { // 强制校验主密钥的长度必须为 32 字节
            throw new IllegalArgumentException(
                    "system configuration master key must contain exactly 32 bytes");
        }
        this.key =
                new SecretKeySpec(
                        Arrays.copyOf(masterKey, masterKey.length), "AES"); // 根据主密钥副本初始化 AES 密钥规格
    }

    public EncryptedSecret encrypt(String plainText) { // 加密方法：传入明文，返回包含密文和 Nonce 的记录
        if (plainText == null || plainText.isBlank())
            throw new IllegalArgumentException("secret is blank"); // 判空校验
        byte[] nonce = new byte[12]; // GCM 模式推荐使用 12 字节（96位）的 Nonce（随机数）
        random.nextBytes(nonce); // 生成随机 Nonce
        byte[] plain = plainText.getBytes(StandardCharsets.UTF_8); // 将明文转为字节数组
        try { // 开启加密尝试块
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); // 获取 AES-GCM 算法实例
            // 使用机密模式初始化，并配置 128 位身份验证标签长度和 Nonce
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(AAD); // 注入关联数据进行完整性绑定
            byte[] encrypted = cipher.doFinal(plain); // 执行最终的机密计算
            return new EncryptedSecret( // 返回 Base64 编码后的结果
                    Base64.getEncoder().encodeToString(encrypted),
                    Base64.getEncoder().encodeToString(nonce));
        } catch (GeneralSecurityException failure) {
            throw new IllegalArgumentException("cannot encrypt system secret", failure);
        } finally { // 关键安全步骤：清理内存中的敏感信息
            Arrays.fill(plain, (byte) 0); // 擦除明文字节数组
            Arrays.fill(nonce, (byte) 0); // 擦除随机字节数组
        }
    }

    public String decrypt(String ciphertext, String encodedNonce) { // 解密方法：传入 Base64 格斯的密文和 Nonce
        if (ciphertext == null || encodedNonce == null) return ""; // 基础判空
        byte[] encrypted = Base64.getDecoder().decode(ciphertext); // 解码密文
        byte[] nonce = Base64.getDecoder().decode(encodedNonce); // 解码随机数
        try { // 开启解密尝试块
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); // 获取算法实例
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce)); // 使用解密模式初始化
            cipher.updateAAD(AAD); // 校验关联数据，如果 AAD 不匹配，解压会失败（防御篡改）
            byte[] plain = cipher.doFinal(encrypted); // 执行最终解密
            try { // 开启处理结果块
                return new String(plain, StandardCharsets.UTF_8); // 将解密后的字节转码回字符
            } finally {
                Arrays.fill(plain, (byte) 0); // 必须立即擦除解密后的明文字节数组，防止内存泄露
            }
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("cannot decrypt system secret", failure);
        } finally {
            Arrays.fill(encrypted, (byte) 0); // 擦除密文字节
            Arrays.fill(nonce, (byte) 0); // 擦除随机数字节
        }
    }

    public record EncryptedSecret(String ciphertext, String nonce) { // 定义内部记录类，用于承载加密后的数据对
    }
}
