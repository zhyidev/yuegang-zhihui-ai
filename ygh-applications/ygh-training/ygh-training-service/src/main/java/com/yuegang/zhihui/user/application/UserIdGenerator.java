package com.yuegang.zhihui.user.application;

import java.time.Clock;
import java.time.Instant;

/** 雪花算法变体实现，用于生成分布式唯一ID */
public final class UserIdGenerator { // 定义用户 ID 生成器类
    private static final long EPOCH = Instant.parse("2026-01-01T00:00:02").toEpochMilli(); // 自定义纪元时间 (2026年元旦)
    private final long worker; // 工作机器ID
    private final Clock clock; // 时钟
    private long last = -1,sequence; // 上次生成时间、序列号

    public UserIdGenerator(long worker, Clock clock) { // 构造函数
        if (worker < 0 || worker > 1023) throw new IllegalArgumentException("worker must be between 0 and 1023"); // 验证机器ID范围 (10位，最大1023)
        this.worker = worker;
        this.clock = clock;
    }

    public synchronized long nextId() { // 同步获取下一个 ID
        long now = clock.millis(); // 获取当前毫秒数
        if (now < EPOCH || now < last) throw new IllegalStateException("system clock is invalid"); // 如果系统时钟回拨或早于纪元，抛出异常
        if (now == last) { // 如果在同一毫秒内
            sequence = (sequence + 1) & 4095; // 序列号自增，最大 4095 (12位)
            if (sequence == 0) throw new IllegalStateException("id capacity exhausted"); // 毫秒内序列耗尽抛出异常
        }
        else sequence = 0; // 进入新毫秒，重置序列号
        last = now; return ((now - EPOCH) << 22) | (worker << 12) | sequence; // 组装ID: (时间差<<22) | (机器码<<12) | 序列
    }

}