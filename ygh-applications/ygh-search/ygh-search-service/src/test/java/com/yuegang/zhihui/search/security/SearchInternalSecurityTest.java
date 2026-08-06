package com.yuegang.zhihui.search.security;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.security.InternalServiceSignature;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchInternalSecurityTest {
    @Test
    void acceptsSignedKnowledgeRequestAndRejectsOthers() {
        byte[] key = "01234567890123456789012345678901".getBytes();
        var security = new SearchInternalSecurity(key);
        var request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/internal/v1/search/index");
        when(request.getHeader("X-YGH-Service")).thenReturn("ygh-knowledge-service");
        String ts = Long.toString(System.currentTimeMillis());
        when(request.getHeader("X-YGH-Service-Timestamp")).thenReturn(ts);
        var signer = new InternalServiceSignature(key, Clock.systemUTC(), Duration.ofSeconds(30));
        String signature = signer.sign(new InternalServiceSignature.Metadata("ygh-knowledge-service", "POST", "/internal/v1/search/index", Instant.ofEpochMilli(Long.parseLong(ts))));
        when(request.getHeader("X-YGH-Service-Signature")).thenReturn(signature);
        assertThatCode(() -> security.verify(request)).doesNotThrowAnyException();
        when(request.getHeader("X-YGH-Service-Signature")).thenReturn("bad");
        assertThatThrownBy(() -> security.verify(request)).isInstanceOf(BusinessException.class);
        when(request.getHeader("X-YGH-Service")).thenReturn("intruder");
        assertThatThrownBy(() -> security.verify(request)).isInstanceOf(BusinessException.class);
        when(request.getHeader("X-YGH-Service")).thenReturn(null);
        assertThatThrownBy(() -> security.verify(request)).isInstanceOf(BusinessException.class);
    }
}
