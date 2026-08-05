package com.yuegang.zhihui.auth.infrastructure;

import com.yuegang.zhihui.auth.domain.CompromisedPasswordChecker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 从类路径加载二进制泄露密码库，并使用二分查找校验密码安全性
 */
public final class ClasspathCompromisedPasswordChecker implements CompromisedPasswordChecker { // 实现泄露密码检查接口

    public static final String DEFAULT_RESOURCE = "/security/common-passwords-sha256.bin"; // 默认资源文件路径
    public static final String DATASET_VERSION = "SecLists-2026.1-xato-top-100000"; // 数据集版本信息
    public static final int EXPECTED_ENTRIES = 96_518; // 预期的记录条数
    public static final String EXPECTED_SHA256 = ""; // 库文件本身的完整性校验哈希 //TODO haxe
    private static final int DIGEST_LENGTH = 32; // SHA-256 摘要长度 (32位字节)
    private final byte[] sortedDigests; // 存储已排序的哈希字节数组

    public ClasspathCompromisedPasswordChecker() { // 默认构造函数
        this(DEFAULT_RESOURCE, EXPECTED_ENTRIES, EXPECTED_SHA256); // 调用全参构造函数
    }

    ClasspathCompromisedPasswordChecker(String resource, int expectedEntries, String expectedSha256) { // 全参构造函数
        Objects.requireNonNull(resource, "resource must not be null"); // 检查资源路径非空
        try (InputStream input = ClasspathCompromisedPasswordChecker.class.getResourceAsStream(resource)) { // 获取资源输入流
            if (input == null) {
                throw new IllegalStateException("compromised-password dataset is unavailable"); // 抛出状态异常：数据集不可用
            }
            sortedDigests = input.readAllBytes(); // 读取所有字节到内存
        } catch (IOException readFailure) { // 捕获读取失败异常
            throw new IllegalStateException("compromised-password dataset cannot be read", readFailure); // 抛出状态异常，数据集无法读取
        }
        // 校验长度是否等于条数乘以每条长度，并校验文件内容的哈希值是否匹配
        if (sortedDigests.length != Math.multiplyExact(expectedEntries, DIGEST_LENGTH)
                || !MessageDigest.isEqual(sha256(sortedDigests), HexFormat.of().parseHex(expectedSha256))) {
            Arrays.fill(sortedDigests, (byte) 0); // 校验失败，擦除内存数据
            throw new IllegalStateException("compromised-password dataset integrity check failed"); // 抛出异常: 完整性检验失败
        }

    }


    @Override
    public boolean isCompromised(char[] password) { // 核心方法:检查密码是否已泄露
        Objects.requireNonNull(password, "password must not be null"); // 检查输入非空
        char[] normalized = password.clone(); // 克隆密码副本用于规范化
        for (int index = 0; index < normalized.length; index++) { // 遍历并转换为小写
            if (normalized[index] >= 'A' && normalized[index] <= 'Z') {
                normalized[index] = (char) (normalized[index] + ('a' - 'A'));
            }
        }
        byte[] encoded = null; // 声明编码后的字节数组
        byte[] digest = null; // 声明计算出的摘要
        ByteBuffer bytes = null; // 声明字节缓冲区
        try {
            bytes = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(normalized)); // 将字符转为UTF-8字符串
            encoded = new byte[bytes.remaining()]; // 创建目标字节数组
            bytes.get(encoded); // 提取字节
            digest = sha256(encoded); // 计算SHA-256哈希
            int low = 0; // 二分查找低位索引
            int high = sortedDigests.length / DIGEST_LENGTH - 1; // 二分查找高位索引
            while (low <= high) { // 执行二分查找
                int middle = (low + high) >>> 1; // 计算中间索引 (防止溢出)
                int comparison = compare(sortedDigests, middle * DIGEST_LENGTH, digest, 0); // 比较中间值与目标哈希
                if (comparison == 0) return true; // 匹配成功，代表已泄露
                if (comparison < 0) low = middle + 1; // 低位向右移动
                else high = middle - 1; // 高位向左移动
            }
            return false; // 没找到，代表暂未泄露
        } catch (CharacterCodingException impossibleUtf8Failure) { // 捕获编码异常
            throw new IllegalStateException("password cannot be UTF-8 encoded", impossibleUtf8Failure); // 抛出异常
        } finally { // 无论如何都要清理敏感数据
            Arrays.fill(normalized, '\0'); // 擦除字符
            if (bytes != null && bytes.hasArray()) Arrays.fill(bytes.array(), (byte) 0); // 擦除缓冲区
            if (encoded != null) Arrays.fill(encoded, (byte) 0); // 擦除编码字节
            if (digest != null) Arrays.fill(digest, (byte) 0); // 擦除摘要字节
        }
    }

    @Override
    public String datasetVersion() { // 捕获数据集版本
        return DATASET_VERSION;
    } // 实现泄露密码检查接口

    private static byte[] sha256(byte[] value) { // 计算SHA-256哈希结构的内部方法
        try {
            return MessageDigest.getInstance("SHA-256").digest(value); // 调用JDK加密库
        } catch (NoSuchAlgorithmException impossible) { // 捕获算法不存在异常
            throw new IllegalStateException("SHA-256 is unavailable", impossible); // 抛出异常
        }
    }

    private static int compare(byte[] left, int leftOffset, byte[] right, int rightOffset) { // 比较两个字节数组切片的内部方法
        for (int index = 0; index < DIGEST_LENGTH; index++) { // 逐字节比较
            int comparison = Integer.compare(
                    Byte.toUnsignedInt(left[leftOffset + index]), Byte.toUnsignedInt(right[rightOffset + index]));
            if (comparison != 0) return comparison; // 发现差异立即返回
        }
        return 0; // 完全一致
    }
}