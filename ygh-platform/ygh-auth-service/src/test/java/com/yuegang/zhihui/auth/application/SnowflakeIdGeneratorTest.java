package com.yuegang.zhihui.auth.application;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnowflakeIdGeneratorTest {
    @Test
    void producesDistinctPositiveIdsForOneWorker() {
        var ids = new SnowflakeIdGenerator(7, Clock.fixed(Instant.parse("2026-07-12T00:00:00Z"), ZoneOffset.UTC));
        assertThat(ids.nextId()).isPositive().isNotEqualTo(ids.nextId());
    }

    @Test
    void rejectsInvalidWorkerAndPreEpochClock() {
        assertThatThrownBy(() -> new SnowflakeIdGenerator(1024, Clock.systemUTC()))
            .isInstanceOf(IllegalArgumentException.class);
        var ids = new SnowflakeIdGenerator(0, Clock.fixed(Instant.parse("2025-12-31T23:59:59Z"), ZoneOffset.UTC));
        assertThatThrownBy(ids::nextId).isInstanceOf(IllegalStateException.class);
    }
}
