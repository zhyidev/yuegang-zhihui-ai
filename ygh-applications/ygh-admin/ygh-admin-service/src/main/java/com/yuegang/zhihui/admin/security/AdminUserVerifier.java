package com.yuegang.zhihui.admin.security;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.security.InternalUserContextSignature;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class AdminUserVerifier {
    private final InternalUserContextSignature s;

    public AdminUserVerifier(byte[] k) {
        s = new InternalUserContextSignature(k, Clock.systemUTC(), Duration.ofSeconds(30));
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
        return new BusinessException(ErrorCode.PERMISSION_DENIED);
    }

    public void verify(HttpServletRequest r) {
        try {
            String u = h(r, "X-YGH-User-Id");
            var roles = v(r.getHeader("X-YGH-Roles"));
            var perms = v(r.getHeader("X-YGH-Permissions"));
            var m = new InternalUserContextSignature.Metadata(u, roles, perms, h(r, "X-Trace-Id"), h(r, "X-Request-Id"), r.getMethod(), r.getRequestURI(), Instant.ofEpochMilli(Long.parseLong(h(r, "X-YGH-User-Context-Timestamp"))));
            if (!s.verify(m, h(r, "X-YGH-User-Context-Signature")) || !roles.contains("ADMIN")) throw f();
        } catch (Exception e) {
            throw f();
        }
    }
}
