package com.yuegang.zhihui.common.mybatis;

import java.util.Objects;

/**
 * 在服务迁移目中发现一个确定性问题
 */
public record MigrationViolation(
    MigrationViolationCode code, // 违规类型代码
    String resourcePath, // 产生违规的文件路径
    String message // 违规的详细信息

) {
    public MigrationViolation { // 校验个字段非空
        Objects.requireNonNull(code, "code must not be null");
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("resourcePath must not be null or blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be null or blank");
        }

    }
}
