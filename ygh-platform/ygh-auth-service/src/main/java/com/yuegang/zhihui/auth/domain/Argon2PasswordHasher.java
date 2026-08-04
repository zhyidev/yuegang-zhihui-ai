package com.yuegang.zhihui.auth.domain;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

/** Argon2 密码哈希实现 */
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
                ||parallelism > MAX_ACCEPTED_PARALLELISM
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


}