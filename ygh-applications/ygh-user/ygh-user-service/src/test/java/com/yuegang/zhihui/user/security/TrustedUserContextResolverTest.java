package com.yuegang.zhihui.user.security;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.security.InternalUserContextSignature;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrustedUserContextResolverTest {
    private static final Instant NOW = Instant.parse("2026-07-12T00:00:00Z");

    private static MockHttpServletRequest signed(byte[] secret, String userId, String path, Instant timestamp) {
        var request = new MockHttpServletRequest("GET", path);
        request.addHeader("X-YGH-User-Id", userId);
        request.addHeader("X-YGH-Roles", "CUSTOMER");
        request.addHeader("X-Trace-Id", "trace-123456");
        request.addHeader("X-Request-Id", "request-123456");
        request.addHeader("X-YGH-User-Context-Timestamp", Long.toString(timestamp.toEpochMilli()));
        var signatures = new InternalUserContextSignature(secret, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(30));
        var metadata = new InternalUserContextSignature.Metadata(userId, List.of("CUSTOMER"), List.of(),
            "trace-123456", "request-123456", "GET", path, timestamp);
        request.addHeader("X-YGH-User-Context-Signature", signatures.sign(metadata));
        return request;
    }

    @Test
    void acceptsOnlyRequestBoundGatewayIdentity() {
        byte[] secret = new byte[32];
        var resolver = new TrustedUserContextResolver(secret, Clock.fixed(NOW, ZoneOffset.UTC));
        var request = signed(secret, "42", "/api/v1/users/me", NOW);
        assertThat(resolver.resolve(request).userId()).isEqualTo("42");
        request.removeHeader("X-YGH-User-Id");
        request.addHeader("X-YGH-User-Id", "43");
        assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    void rejectsExpiredAndMissingSignature() {
        byte[] secret = new byte[32];
        var resolver = new TrustedUserContextResolver(secret, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> resolver.resolve(signed(secret, "42", "/api/v1/users/me", NOW.minusSeconds(31))))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> resolver.resolve(new MockHttpServletRequest("GET", "/api/v1/users/me")))
            .isInstanceOf(BusinessException.class);
    }
}
