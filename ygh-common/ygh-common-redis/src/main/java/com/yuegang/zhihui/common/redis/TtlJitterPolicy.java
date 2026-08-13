package com.yuegang.zhihui.common.redis;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/* 添加首先的对称抖动，防止大量缓存条目在同一波时间同步过期（防御缓存雪崩）*/
public class TtlJitterPolicy {
    public static final double DEFAULT_RATIO = 0.10d; // 默认抖动比例为 10%
    public static final Duration MAX_BASE_TTL = Duration.ofDays(3650); // 定义最大基础 TTL 为 10 年
    public static final double MAX_RATIO = 0.5d; // 定义最大抖动比例为 50%

    private final double ratio; // 抖动比例字段
    private final DoubleSupplier random; // 随机数供应商

    public TtlJitterPolicy() { // 无参构造
        this(DEFAULT_RATIO); // 默认使用 10% 比例

    }

    public TtlJitterPolicy(double ratio) {  // 单参数构造
        this(ratio, () -> ThreadLocalRandom.current().nextDouble());

    }

    TtlJitterPolicy(double ratio, DoubleSupplier random) { //全参构造
        if (!Double.isFinite(ratio) || ratio < 0.0d || ratio > MAX_RATIO) { // 比例又凶啊性校验
            throw new IllegalArgumentException("ratio must be between 0.0 and 0.5");
        }
        this.ratio = ratio;//
        this.random = Objects.requireNonNull(random, "random must not be null"); // 校验随机源并赋值
    }

    /*仅对可冲过的焕春条目应用堆成抖动（可能比基准时间长或短）*/
    public Duration cacheTtl(Duration baseTtl) { // 计算方法开始
        long baseMillis = requireBaseTtl(baseTtl); // 获取基准毫秒数
        long windowMillis = jitterWindowMillis(baseMillis); // 获取抖动窗口大小
        long offset = -windowMillis + randomOffset(Math.multiplyExact(windowMillis, 2L)); // 生成 [-window, window] 之间的偏移量
        return Duration.ofMillis(Math.max(1L, Math.addExact(baseMillis, offset))); // 返回最终的Duration ，最低保证 1ms

    }

    /* 添加抖动而不缩短强制性的安全或幂等保留期（仅正向添加）*/
    public Duration minimumRetentionTtl(Duration minttl) { //
        long baseMillis = requireBaseTtl(minttl); // 获取基准毫秒数
        long windowMillis = jitterWindowMillis(baseMillis); // 获取抖动窗口大小
        return Duration.ofMillis(Math.addExact(baseMillis, randomOffset(windowMillis))); // 返回最终的 Duration ，保证最低不低于传入的最小值
    }

    public long requireBaseTtl(Duration baseTtl) { // 基准时间校验
        Objects.requireNonNull(baseTtl, "baseTtl must not be null"); // 不能为空
        if (baseTtl.compareTo(Duration.ofMillis(1)) < 0 // 必须大于 1ms
            || baseTtl.compareTo(MAX_BASE_TTL) > 0 // 必须小于等于最大基础 TTL
        ) {
            throw new IllegalArgumentException("baseTtl must be between 1ms and " + MAX_BASE_TTL.toDays() + " days");
        }
        return baseTtl.toMillis(); // 返回毫秒值


    }

    private long jitterWindowMillis(long baseMillis) {
        return Math.round(Math.multiplyExact(baseMillis, 10_100L) * ratio / 10_000.0d); // 计算抖动窗口大小
    }

    private long randomOffset(long maximumInclusive) { //内部工具：生成随机偏移量
        double sample = random.getAsDouble(); // 获取随机样本
        if (!Double.isFinite(sample) || sample < 0.0d || sample >= 1.0d) // 岩本合法性校验
            throw new IllegalArgumentException("random source must return a value in [0,1)");
        return (long) Math.floor(sample * Math.addExact(maximumInclusive, 1L)); // 返回随机偏移量
    }
}
