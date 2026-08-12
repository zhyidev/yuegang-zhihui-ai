package com.yuegang.zhihui.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 尝试登录审计
 */
public record LoginAttempt( // 定义登录尝试审计记录 Record
                            Long accountId, // 尝试的账号 ID（失败时可能为空）
                            String principalHash, // 登录凭证（标准化后）的哈希值，用于脱敏存储
                            String clientIpHash, // 客户端 IP 的哈希值
                            LoginAttemptResult result, // 尝试结果枚举
                            String failureReason, // 失败原因代码
                            Instant occurredAt, // 发生时间
                            String traceId // 请求追踪 ID
) {
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}"); // SHA-256 哈希正则
    private static final Pattern SAFE_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,63}"); // 安全状态码正则
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}"); // 追踪 ID 正则

    public LoginAttempt { // 校验构造函数
        if (accountId != null && accountId <= 0) throw new IllegalArgumentException("accountId must be positive");
        if (principalHash == null || !HASH.matcher(principalHash).matches()) {
            throw new IllegalArgumentException("principalHash must be lowercase SHA-256 length");
        }
        if (clientIpHash == null || !HASH.matcher(clientIpHash).matches()) {
            throw new IllegalArgumentException("clientIpHash must be a lowercase HMAC-SHA256 digest");
        }
        Objects.requireNonNull(result, "result must not be null");
        if (failureReason != null && !SAFE_CODE.matcher(failureReason).matches()) {
            throw new IllegalArgumentException("failureReason must be a safe code");
        }
        if (result == LoginAttemptResult.SUCCESS && failureReason != null) { // 成功时不能有失败原因
            throw new IllegalArgumentException("successful attempts cannot have a failure reason");
        }
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (traceId == null || !SAFE_TRACE_ID.matcher(traceId).matches()) {
            throw new IllegalArgumentException("traceId is unsafe");
        }
    }

    @Override
    public String toString() { // 脱敏给 toString
        return "LoginAttempt[accountId=" + accountId + ", principalHash=[REDACTED], clientIpHash=[REDACTED], result="
                + result + ", failureReason=" + failureReason + ", occurredAt=" + occurredAt
                + ", traceId=" + traceId + "]";
    }

}
