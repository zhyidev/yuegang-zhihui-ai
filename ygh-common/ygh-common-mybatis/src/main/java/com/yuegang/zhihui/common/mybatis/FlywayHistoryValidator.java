package com.yuegang.zhihui.common.mybatis;

import org.flywaydb.core.Flyway;

import java.util.Objects;

/**
 * 将校验和(Checksum）及应用历史的完整性检查委托给Flyway公共API。
 */
public final class FlywayHistoryValidator {

    public void validateOrThrow(Flyway flyway) { // 检验已执行的迁移记录
        Objects.requireNonNull(flyway, "flyway must not be null");
        var result = flyway.validateWithResult(); // 应用 Flyway 原生校验
        if (!result.validationSuccessful) { // 如果校验不容过（如果本地脚本被改动导致 Checksum 变化
            throw new MigrationPolicyException(
                MigrationViolationCode.HISTORY_VALIDATION_FAILED,
                "Flyway history validation failed with"
                    + result.invalidMigrations.size() + "invalid migration(s)"
            ); // 抛出异常并提示无效数据

        }

    }
}
