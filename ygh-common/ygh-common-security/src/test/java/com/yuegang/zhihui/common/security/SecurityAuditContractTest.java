package com.yuegang.zhihui.common.security;

import org.assertj.core.api.AtomicReferenceArrayAssert;
import org.junit.jupiter.api.Test;

import java.security.Security;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SecurityAuditContractTest {
    @Test
    void shouldExposeStablePerissionDeniedBusinessCode(){
        PermissionDeniedException exception = new PermissionDeniedException();

        assertThat(exception.errorCode().code()).isEqualTo("PERMISSION_DENIED");
    }

    @Test
    void shouldPublishImmutabSecuritDecisionWithoutSensitiveCredentials(){
        AtomicReference<SecurityAuditEvent> captured = new AtomicReference<>();
        SecurityAuditPublisher publisher = captured::set;;
        SecurityAuditEvent event = new SecurityAuditEvent(
          Instant.parse("2026-07-17T:08:00:00z"),
          "user-1",
          "READ_ADDRESS",
          "user:address:read",
          SecurityDecision.DENIED,
          "RESOURCE_NOT_FOUND",
          "trace-1"
        );

        publisher.publish(event);

        assertThat(captured.get()).isEqualTo(event);

        assertThat(event.toString()).doesNotContain("token","password","secret");
    }
}
