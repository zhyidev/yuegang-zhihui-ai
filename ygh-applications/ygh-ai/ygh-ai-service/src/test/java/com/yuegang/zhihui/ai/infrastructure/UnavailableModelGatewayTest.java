package com.yuegang.zhihui.ai.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnavailableModelGatewayTest {
    @Test
    void failsClosedWithoutPretendingToBeAConfiguredModel() {
        var gateway = new UnavailableModelGateway();
        assertThat(gateway.modelName()).isEqualTo("unavailable");
        assertThat(gateway.answer("system", "question")).contains("未配置");
    }
}
