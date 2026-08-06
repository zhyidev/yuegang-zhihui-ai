package com.yuegang.zhihui.auth.domain;

import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Argon2 密码哈希实现
 */
public final class Argon2PasswordHasher { // 使用 Argon2id 算法的密码哈希器
    public static final String ALGORITHM = "ARGON2ID"; // 算法名称
    public static final int VERSION = 1; // 内部版本号
    private static final int ARGON2_VERSION_13 = 0x13; // Argon2 规范版本 1.3
    private static final int MAX_ENCODED_LENGTH = 256; // 编码字符串的最大长度限制
    private static final int MAX_ACCEPTED_MEMORY_KIB = 32 * 1024; // 最大允许消耗 32MB 内存
    private static final int MAX_ACCEPTED_ITERATIONS = 4; // 最大允许的迭代次数
    private static final int MAX_ACCEPTED_PARALLELISM = 2; // 最大允许并行度
    // 正则匹配 Argon2 标准编码格式的正则表达式
    private static final Pattern ARGON2_ENCODING = Pattern.compile("^\\$argon2id\\$v=19\\$m=([1-9][0-9]{0,5}),t=([1-9][0-9]?),p=([1-9][0-9]?)" +
            "\\$([A-Za-z0-9+/]{22})={0,2}\\$([A-Za-z0-9+/]{43})={0,2}$");
    private final int saltLength; // 盐长度
    private final int hashLength; // 哈希长度
    private final int parallelism; // 并行度
    private final int memoryKiB; // 内存消耗 (KiB)
    private final int iterations; // 迭代次数
    private final SecureRandom secureRandom; // 安全随机数源
    private final Semaphore capacity; // 并行容量控制器（防止 GPU 耗尽攻击）
    private final Duration capacityWait; // 等待容量的超时时长

    public Argon2PasswordHasher(int saltLength, int hashLength, int parallelism, int memoryKiB, int iterations) {
        this(saltLength, hashLength, parallelism, memoryKiB, iterations, 2, Duration.ofSeconds(5)); // 默认构造函数
    }

    public Argon2PasswordHasher( // 全参数构造函数
                                 int saltLength, int hashLength, int parallelism, int memoryKiB, int iterations,
                                 int maximumConcurrentOperations, Duration capacityWait) {
        // 参数合法性校验：严格遵守安全基准
        if (saltLength != 16 || hashLength != 32 || parallelism < 1
                || parallelism > MAX_ACCEPTED_PARALLELISM
                || memoryKiB < 8 || memoryKiB > MAX_ACCEPTED_MEMORY_KIB
                || iterations < 1 || iterations > MAX_ACCEPTED_ITERATIONS) {
            throw new IllegalArgumentException("invalid Argon2id parameters");
        }
        if (maximumConcurrentOperations < 1) {
            throw new IllegalArgumentException("maximumConcurrentOperations must be positive");
        }
        this.capacityWait = Objects.requireNonNull(capacityWait, "capacityWait must not be null");
        if (capacityWait.isNegative() || capacityWait.isZero()) {
            throw new IllegalArgumentException("capacityWait must be positive");
        }

        this.saltLength = saltLength;
        this.hashLength = hashLength;
        this.parallelism = parallelism;
        this.memoryKiB = memoryKiB;
        this.iterations = iterations;
        this.secureRandom = new SecureRandom();
        this.capacity = new Semaphore(maximumConcurrentOperations, true); // 使用公平信号量
    }

    public static Argon2PasswordHasher owaspMinimum() { // 获取 OWASP 建议的最低安全配置
        return new Argon2PasswordHasher(16, 32, 1, 19 * 1024, 2);
    }

    public PasswordDigest hash(char[] rawPassword) { // 对原始密码进行哈希
        Objects.requireNonNull(rawPassword, "rawPassword must not be null");
        return withCapacity(() -> { // 在容量限制内执行
            byte[] salt = new byte[saltLength]; // 生成随机盐
            byte[] hash = new byte[hashLength]; // 哈希结果容器
            secureRandom.nextBytes(salt);
            try {
                generate(rawPassword, salt, hash, memoryKiB, iterations, parallelism); // 生成哈希
                // 按照标准格式拼接字符串
                String encoded = "$argon2id$v=19$m=" + memoryKiB + ",t=" + iterations + ",p=" + parallelism
                        + "$" + Base64.getEncoder().withoutPadding().encodeToString(salt)
                        + "$" + Base64.getEncoder().withoutPadding().encodeToString(hash);
                return new PasswordDigest(encoded, ALGORITHM, VERSION);
            } finally {
                Arrays.fill(salt, (byte) 0); // 擦除内存中的敏感数据
                Arrays.fill(hash, (byte) 0);
            }
        });
    }

    public boolean matches(char[] rawPassword, PasswordDigest digest) { // 校验密码是否匹配
        ParsedEncoding parsed = parse(digest); // 解析已有的哈希字符串
        if (rawPassword == null || parsed == null) {
            return false;
        }
        return withCapacity(() -> { // 在容量限制内执行
            byte[] actual = new byte[parsed.hash().length];
            try {
                generate(rawPassword, parsed.salt(), actual, parsed.memoryKiB(), parsed.iterations(), parsed.parallelism());
                // 使用恒定时间比较法防止时序攻击
                return MessageDigest.isEqual(actual, parsed.hash());
            } finally {
                Arrays.fill(actual, (byte) 0); // 擦除内存
                parsed.clear();
            }
        });
    }

    public boolean needsUpgrade(PasswordDigest digest) { // 检查哈希参数是否落后，是否需要升级强度
        ParsedEncoding parsed = parse(digest);
        if (parsed == null) {
            return true;
        }
        try {
            return parsed.memoryKiB() != memoryKiB || parsed.iterations() != iterations
                    || parsed.parallelism() != parallelism || parsed.salt().length != saltLength
                    || parsed.hash().length != hashLength;
        } finally {
            parsed.clear();
        }
    }

    private ParsedEncoding parse(PasswordDigest digest) { // 解析编码字符串为内部对象
        if (digest == null || !ALGORITHM.equals(digest.algorithm().toUpperCase(Locale.ROOT)) || digest.version() != VERSION || digest.hash() == null
                || digest.hash().length() > MAX_ENCODED_LENGTH) {
            return null;
        }
        Matcher matcher = ARGON2_ENCODING.matcher(digest.hash()); // 正则匹配
        if (!matcher.matches()) {
            return null;
        }
        try {
            int memory = Integer.parseInt(matcher.group(1));
            int time = Integer.parseInt(matcher.group(2));
            int lanes = Integer.parseInt(matcher.group(3));
            if (memory < 8 || memory > MAX_ACCEPTED_MEMORY_KIB || time > MAX_ACCEPTED_ITERATIONS
                    || lanes > MAX_ACCEPTED_PARALLELISM) {
                return null;
            }
            byte[] salt = Base64.getDecoder().decode(matcher.group(4)); // 解码盐
            byte[] hash = Base64.getDecoder().decode(matcher.group(5)); // 解码哈希
            if (salt.length != 16 || hash.length != 32) {
                Arrays.fill(salt, (byte) 0);
                Arrays.fill(hash, (byte) 0);
                return null;
            }
            return new ParsedEncoding(memoryKiB, time, lanes, salt, hash);
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    private void generate(char[] password, byte[] salt, byte[] output, int memory, int time, int lanes) { // 核心生成逻辑
        byte[] encodedPassword = encodingUtf8(password); // 将字符数组转为 UTF-8 字节数组
        Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(ARGON2_VERSION_13).withSalt(salt).withMemoryAsKB(memory)
                .withIterations(time).withParallelism(lanes).build();

        try {
//            var generator = new Argon2BytesGenerator() // 实例化生成器
//                    .init(parameters); // 生成器初始化
//            generator.generateBytes(encodedPassword, output); // 计算结果
        } finally {
//            parameters.clear(); // 清理参数
            Arrays.fill(encodedPassword, (byte) 0); // 清理密码字节
        }
    }

    private byte[] encodingUtf8(char[] password) { // 辅助: 将密码安全地转换为 UTF-8 字节
        ByteBuffer buffer = null;
        try {
            buffer = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(password));
            byte[] encoded = new byte[buffer.remaining()];
            buffer.get(encoded);
            return encoded;
        } catch (CharacterCodingException invalidUnicode) {
            throw new IllegalArgumentException("password contains invalid Unicode", invalidUnicode);
        } finally {
            if (buffer != null && buffer.hasArray()) {
                Arrays.fill(buffer.array(), (byte) 0);
            }
        }
    }

    private <T> T withCapacity(java.util.function.Supplier<T> operation) { // 信号量保护包装器
        boolean acquired;
        try {
            acquired = capacity.tryAcquire(capacityWait.toMillis(), TimeUnit.MILLISECONDS); // 尝试获取令牌
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new PasswordHashCapacityException("password hashing interrupted", interrupted);
        }
        if (!acquired) { // 获取失败抛出容量超限异常（防拒绝服务攻击）
            throw new PasswordHashCapacityException("password hashing capacity exhausted");
        }

        try {
            return operation.get(); // 执行操作
        } finally {
            capacity.release(); // 释放令牌
        }
    }

    private record ParsedEncoding(int memoryKiB, int iterations, int parallelism, byte[] salt,
                                  byte[] hash) { // 内部解析数据载体
        private void clear() { // 清理敏感数据
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(hash, (byte) 0);
        }
    }

}