package com.yuegang.zhihui.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.nio.charset.StandardCharsets;

public final class InternalServiceSignature {
    private final SecretKeySpec key;
    private final Clock clock;
    private final Duration skew;

    public InternalServiceSignature(byte[] s, Clock c, Duration d) {
        if (s == null || s.length < 32) {
            throw new IllegalArgumentException("service secret too short");
        }
        key = new SecretKeySpec(Arrays.copyOf(s, s.length), "HmacSHA256");
        clock = Objects.requireNonNull(c);
        skew = Objects.requireNonNull(d);
    }

    public String sign(Metadata m) {
        return HexFormat.of().formatHex(mac(canonical(m)));
    }

    public boolean verify(Metadata m, String sig) {
        if (sig == null || !sig.matches("[0-9a-f]{64}") ||
                Duration.between(m.timestamp(), clock.instant()).abs().compareTo(skew) > 0) {
            return false;
        }
        byte[] a = HexFormat.of().parseHex(sig);
        byte[] e = mac(canonical(m));
        try {
            return MessageDigest.isEqual(a, e);
        } finally {
            Arrays.fill(a, (byte) 0);
            Arrays.fill(e, (byte) 0);
        }
    }

    private byte[] canonical(Metadata m) {
        return String.join("\n",
                m.service(),
                m.method(),
                m.path(),
                Long.toString(m.timestamp().toEpochMilli())
        ).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] mac(byte[] b) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(b);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            Arrays.fill(b, (byte) 0);
        }
    }

    public record Metadata(String service, String method, String path, Instant timestamp) {
        public Metadata {
            if (service == null || !service.matches("[a-z]{3,6}-{2,63}")) {
                throw new IllegalArgumentException("unsafe service");
            }
            if (method == null || !method.matches("[A-Z]{3,10}")) {
                throw new IllegalArgumentException("unsafe method");
            }
            if (path == null || !path.startsWith("/")) {
                throw new IllegalArgumentException("unsafe path");
            }
            Objects.requireNonNull(timestamp);
        }
    }
}