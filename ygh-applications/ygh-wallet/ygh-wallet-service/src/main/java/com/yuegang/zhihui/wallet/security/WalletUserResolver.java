package com.yuegang.zhihui.wallet.security;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.security.InternalUserContextSignature;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class WalletUserResolver {
    private final InternalUserContextSignature s;

    public WalletUserResolver(byte[] k, Clock c) {
        s = new InternalUserContextSignature(k, c, Duration.ofSeconds(30));
    }

    private static String h(HttpServletRequest r, String n) {
        String v = r.getHeader(n);
        if (v == null || v.isBlank()) throw f();
        return v;
    }

    private static List<String> v(String x) {
        return x == null || x.isBlank() ? List.of() : List.of(x.split(","));
    }

    private static BusinessException f() {
        return new BusinessException(ErrorCode.UNAUTHENTICATED);
    }

    public long resolve(HttpServletRequest r) {
        try {
            String u = h(r, "X-YGH-User-Id");
            var roles = v(r.getHeader("X-YGH-Roles"));
            var perms = v(r.getHeader("X-YGH-Permissions"));
            var m = new InternalUserContextSignature.Metadata(u, roles, perms, h(r, "X-Trace-Id"), h(r, "X-Request-Id"), r.getMethod(), r.getRequestURI(), Instant.ofEpochMilli(Long.parseLong(h(r, "X-YGH-User-Context-Timestamp"))));
            if (!s.verify(m, h(r, "X-YGH-User-Context-Signature"))) throw f();
            long id = Long.parseLong(u);
            if (id <= 0) throw f();
            return id;
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw f();
        }
    }
}
