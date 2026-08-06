package com.yuegang.zhihui.wallet.application;

import com.yuegang.zhihui.common.mq.JdbcOutboxDispatcher;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WalletOutboxConfigurationTest {
    @Test
    void scheduledJobDelegatesToDispatcher() {
        var dispatcher = mock(JdbcOutboxDispatcher.class);
        new WalletOutboxConfiguration.WalletOutboxJob(dispatcher).dispatch();
        verify(dispatcher).dispatch();
    }
}
