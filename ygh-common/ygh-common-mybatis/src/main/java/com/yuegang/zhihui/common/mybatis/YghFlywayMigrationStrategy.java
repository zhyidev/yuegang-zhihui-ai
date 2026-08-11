package com.yuegang.zhihui.common.mybatis;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.InfoOutput;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 最终迁移入口：在委派给 Flyway 执行迁移前，验证有效配置，解析出的资源已应用的历史
 */
public class YghFlywayMigrationStrategy implements FlywayMigrationStrategy { // 拦截 Spring 自动执行的迁移

    private final FlywayMigrationPolicy migrationPolicy; // 命名内容策略
    private final FlywayConfigurationGuard configurationGuard; // 配置安全首位
    private final FlywayHistoryValidator historyValidator; // 历史完整性校验

    public YghFlywayMigrationStrategy(
        FlywayMigrationPolicy migrationPolicy,
        FlywayConfigurationGuard configurationGuard,
        FlywayHistoryValidator historyValidator
    ) {
        this.migrationPolicy = Objects.requireNonNull(migrationPolicy, "migrationPolicy must not be null");
        this.configurationGuard = Objects.requireNonNull(configurationGuard, "configurationGuard must not be null");
        this.historyValidator = Objects.requireNonNull(historyValidator, "historyValidator must not be null");
    }

    // --- 内部辅助判断方法 ---
    private static boolean isRepeatable(InfoOutput migration) {
        boolean repeatableCategory = migration.category != null &&
            migration.category.toLowerCase().contains("repeatable");
        boolean SqlWithoutVersion = resolvedVersion(migration) == null &&
            migration.filepath != null && migration.filepath.toLowerCase().endsWith(".sql");
        return repeatableCategory || SqlWithoutVersion;
    }

    private static boolean isUndo(InfoOutput migration) { // 判断是否为 Undo
        if (migration.filepath == null) return false;
        String normalized = migration.filepath.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        String fileName = separator >= 0 ? normalized.substring(separator + 1) : normalized;
        return fileName.startsWith("U");


    }

    private static boolean isSqlMigration(InfoOutput migration) { // 判断是否为 SQL 类型
        return migration.type != null && migration.type.equalsIgnoreCase("SQL");

    }

    private static long parseAppliedVersion(String version) { // 解析版本号为 long
        try {
            return Long.parseLong(version);
        } catch (NumberFormatException exception) {
            throw new MigrationPolicyException(
                MigrationViolationCode.INVALID_NAME,
                "applied migration uses unsupported version format");
        }
    }

    private static String resolvedVersion(InfoOutput migration) { // 获取版本对应的版本号
        if (migration.rawVersion != null && !migration.rawVersion.isBlank())
            return migration.rawVersion;
        return migration.version == null || migration.version.isBlank() ? null : migration.version;
    }

    private static boolean isApplied(InfoOutput migration) {
        if (migration.installedOnUTC != null && !migration.installedOnUTC.isBlank()) return true;
        return migration.state != null &&
            migration.state.equalsIgnoreCase("Success");
    }

    @Override
    public void migrate(Flyway flyway) { // 核心迁移，逻辑拦截
        Objects.requireNonNull(flyway, "flyway must not be null");
        configurationGuard.validateOrThrow(flyway.getConfiguration()); // 1. 校验配置是否安全

        var info = flyway.info().getInfoResult(); // 2. 获取flyway已应用的迁移历史
        List<String> allResources = new ArrayList<>(); // 前部资源
        List<String> notAppliedResources = new ArrayList<>(); // 未应用版本
        long highestAppliedVersion = 0L; // 数据库已经应用最高版本

        for (InfoOutput migration : info.migrations) { // 迭代所有脚本
            if (!isSqlMigration(migration)) { // 强制要求只是使用SQL迁移，禁止 Java 迁移（防止隐式漏洞)
                throw new MigrationPolicyException(MigrationViolationCode.UNSUPPORTED_MIGRATION_TYPE, "Java migration is forbidden");
            }
            if (isRepeatable(migration)) { // 再次检验并拦截 R__ 脚本
                throw new MigrationPolicyException(MigrationViolationCode.REPEATABLE_SCRIPT_FORBIDDEN, "repeatable migration is forbidden");
            }
            if (isUndo(migration)) {
                throw new MigrationPolicyException(MigrationViolationCode.UNDO_SCRIPT_FORBIDDEN, "undo migration is forbidden");
            }
            if (migration.filepath != null && migration.filepath.endsWith(".sql")) { // 只处理 SQL 脚本
                String policyPath = configurationGuard.toPolicyResourcePath(flyway.getConfiguration(), migration.filepath);
                allResources.add(policyPath);
                if (migration.installedOnUTC == null) { // 如果尚未安装，假如待执行列表
                    notAppliedResources.add(policyPath);

                }
            }
            String resolvedVersion = resolvedVersion(migration);
            if (resolvedVersion != null && isApplied(migration)) { // 计算历史最高版本号
                highestAppliedVersion = Math.max(highestAppliedVersion, parseAppliedVersion(resolvedVersion));
            }
        }
        migrationPolicy.validate(allResources).throwIfInvalid(); // 2.执行静态规范验证
        for (InfoOutput migration : info.migrations) { // 3. 检查解析用的版本号是否低于历史版本
            String resourceVersion = resolvedVersion(migration);
            if (resourceVersion != null && parseAppliedVersion(resourceVersion) <= highestAppliedVersion) {
                throw new MigrationPolicyException(MigrationViolationCode.OUT_OF_ORDER_VERSION, "migration version is outdated: " + resourceVersion);

            }
        }
        migrationPolicy.validateNewMigrations(notAppliedResources, highestAppliedVersion).throwIfInvalid();// 4. 检验脚本是否合法
        flyway.migrate(); // 5. 正式执行 Flyway 迁移
        historyValidator.validateOrThrow(flyway); // 6. 钱以后再次校验历史一致性
    }
}
