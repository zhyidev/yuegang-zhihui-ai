package com.yuegang.zhihui.wallet.security;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.security.InternalServiceSignature;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class WalletInternalVerifier {
    private final InternalServiceSignature signatures;

    public WalletInternalVerifier(byte[] k) {
        signatures = new InternalServiceSignature(k, Clock.systemUTC(), Duration.ofSeconds(30));
    }

    private static String h(HttpServletRequest r, String n) {
        String v = r.getHeader(n);
        if (v == null || v.isBlank()) throw denied();
        return v;
    }

    private static BusinessException denied() {
        return new BusinessException(ErrorCode.UNAUTHENTICATED);
    }

    public void verify(HttpServletRequest r) {
        try {
            String service = h(r, "X-YGH-Service");
            Instant at = Instant.ofEpochMilli(Long.parseLong(h(r, "X-YGH-Service-Timestamp")));
            if (!signatures.verify(new InternalServiceSignature.Metadata(service, r.getMethod(), r.getRequestURI(), at), h(r, "X-YGH-Service-Signature")))
                throw denied();
        } catch (Exception e) {
            throw denied();
        }
    }
}
