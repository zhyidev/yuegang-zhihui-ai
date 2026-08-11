package com.yuegang.zhihui.common.mybatis;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;

/**
 * 为每个业务服务强制执行"仅向前，快速失败"的 Flyway 设置。
 */
public final class YghFlywaySafetyCustomizer implements FlywayConfigurationCustomizer { // 解耦 MyBatis Plus 与业务 API
    @Override
    public void customize(FluentConfiguration configuration) {
        configuration
                .validateMigrationNaming(true) // 强制启用命名校验
                .validateOnMigrate(true) // 强制启用迁移校验
                .cleanDisabled(true) // 禁止清理数据库
                .baselineOnMigrate(false) // 禁止在迁移时基线化
                .ignoreMigrationPatterns(new String[0]); // 禁止忽略任何版本
    }
}
