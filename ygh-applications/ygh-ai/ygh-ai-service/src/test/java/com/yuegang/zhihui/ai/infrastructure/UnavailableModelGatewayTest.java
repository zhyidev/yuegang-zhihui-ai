package com.yuegang.zhihui.ai.infrastructure;

import org.junit.jupiter.api.Test;

class UnavailableModelGatewayTest {
    @Test
    void failsClosedWithoutPretendingToBeAConfiguredModel() {
        var gateway = new UnavailableModelGateway();
        assertThat(gateway.modelName()).isEqualTo("unavailable");
        assertThat(gateway.answer("system", "question")).contains("未配置");
    }
}
