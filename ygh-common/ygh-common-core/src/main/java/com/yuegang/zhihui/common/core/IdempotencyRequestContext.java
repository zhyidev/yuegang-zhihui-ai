package com.yuegang.zhihui.common.core;

import java.time.OffsetDateTime;

/**单个调用者操作及其规范请求具体的身份标识*/
public record IdempotencyRequestContext( // 定义幂等请求载体
                                         String idempotencyKey,     // 幂等键
                                         String operation,          // 业务操作名称
                                         String subjectId,          // 请求主题ID
                                         String requestFingerprint, // 请求参数的哈希指纹
                                         OffsetDateTime requestedAt // 请求发起时间戳

) {
    public IdempotencyRequestContext{ // 校验非空
        requireText(idempotencyKey,"idempotencyKey");
        requireText(operation,"operation");
        requireText(subjectId,"subjectId");
        requireText(requestFingerprint,"requestFingerprint");
        if (requestedAt == null) {
            throw new IllegalArgumentException("requestedAt must not be null");
        }
    }

    private static void requireText(String value,String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "must not be blank");
        }

    }
}
