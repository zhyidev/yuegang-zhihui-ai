package com.yuegang.zhihui.common.mybatis;

import java.util.*;
import java.util.regex.Pattern;


/**
 * 针对单个服务拥有的数据库，实施代码库级别的“仅向前”迁移策列。
 * 数据库历史记录的物理检查仍由 Flyway 自行完成。
 */
public final class FlywayMigrationPolicy { // 强制执行脚本命名和内容规范

    private static final Pattern VERSIONED_MIGRATION = Pattern.compile(
            "^db/migration/V([1-9][0-9]*)_([a-z][a-z0-9]*(?:_[a-z0-9]+)*)\\.sql$"
    );

    public FlywayMigrationPolicy() {
    }

    public MigrationDescriptor migrationDescriptor(String resourcePath) { //解析脚本路径为描述对象
        Objects.requireNonNull(resourcePath, "resourcePath must not be null");
        String normalized = normalize(resourcePath); // 规范化斜杠
        if (normalized.isBlank() || normalized.contains("../") || normalized.contains("/../")) { //禁止路径穿越攻击
            throw new MigrationPolicyException(
                    MigrationViolationCode.UNDO_SCRIPT_FORBIDDEN,
                    "undo migration is forbidden:" + safeDisplay(normalized)
            );
        }
        if (normalized.startsWith("db/migration/R__")) { // 禁止可重复性执行脚本 （R__) ， 保证所有结构变更均有唯一版本号
            throw new MigrationPolicyException(
                    MigrationViolationCode.REPEATABLE_SCRIPT_FORBIDDEN,
                    "repeatable migration is forbidden:" + safeDisplay(normalized)
            );
        }
        var matcher = VERSIONED_MIGRATION.matcher(normalized); // 执行正则匹配
        if (!matcher.matches()) { // 如果不匹配
            throw invalid(normalized); // 命名不合规
        }

        long version;
        try {
            version = Long.parseLong(matcher.group(1)); // 解析版本号
        } catch (NumberFormatException exception) {
            throw invalid(normalized); // 如果解析失败，抛出异常
        }
        String description = matcher.group(2); // 解析描述
        return new MigrationDescriptor(normalized, version, description); // 返回描述对象
    }

    //注意别的（老师的）是validate
    public MigrationValidationReport validate(Collection<String> resourcePaths) { // 批量验证脚本列表
        Objects.requireNonNull(resourcePaths, "resourcePaths must not be null");
        var violations = new ArrayList<MigrationViolation>(); // 违规列表
        Set<String> seenPath = new HashSet<>(); // 已经看到的路径集合
        Map<Long, String> pathByVersion = new HashMap<>(); // 版本号到路径的映射

        for (String resourcePath : resourcePaths) { // 遍历所有路径
            if (resourcePath == null) {
                violations.add(new MigrationViolation(
                        MigrationViolationCode.INVALID_NAME,
                        "Invalid resource path: <null>",
                        "migration path must not be null"
                ));
                continue;
            }

            String normalized = normalize(resourcePath);
            if (!seenPath.add(normalized)) {
                String displayPath = safeDisplay(normalized);
                violations.add(new MigrationViolation(
                        MigrationViolationCode.DUPLICATE_VERSION,
                        displayPath,
                        "Duplicate migration path: " + displayPath
                ));
                continue;
            }
            try {
                var descriptor = migrationDescriptor(normalized); // 解析命名
                String existingPath = pathByVersion.putIfAbsent(descriptor.version(), descriptor.resourcePath()); // 检查版本号是否重复
                if (existingPath != null) {
                    violations.add(new MigrationViolation(
                            MigrationViolationCode.DUPLICATE_VERSION,
                            descriptor.resourcePath(),
                            "Duplicate migration version: " + descriptor.version() + " in " + (existingPath)));
                }
            } catch (MigrationPolicyException exception) { // 捕获迁移策略异常

            }

        }
        return new MigrationValidationReport(violations); // 返回汇总表
    }

    public MigrationValidationReport validateNewMigrations(Collection<String> resourcePaths, long highestAppliedVersion) { // 验证新脚本是否满足
        if (highestAppliedVersion < 0) {
            throw new IllegalArgumentException("highestAppliedVersion must not be negative");
        }
        var violations = new ArrayList<>(validate(resourcePaths).migrationViolationlist()); // 先执行基础静态验证

        for (String resourcePath : resourcePaths) {// 遍历所有路径
            if (resourcePath == null) continue;
            try {
                var descriptor = migrationDescriptor(resourcePath); // 解析描述对象
                if (descriptor.version() <= highestAppliedVersion) { // 如果版本号小于等于已应用的最高版本号，则违规
                    violations.add(new MigrationViolation(
                            MigrationViolationCode.OUT_OF_ORDER_VERSION,
                            descriptor.resourcePath(),
                            "Migration version " + descriptor.version() + " is not greater than highest applied version " + highestAppliedVersion
                    ));
                }
            } catch (MigrationPolicyException exception) {

            }
        }
        return new MigrationValidationReport(violations); // 返回汇总表
    }

    private static String normalize(String resourcePath) {
        return resourcePath.replace('\\', '/'); //统一斜杠
    }

    public static MigrationPolicyException invalid(String path) { // 命名错误工厂
        String displayPath = safeDisplay(path);
        return new MigrationPolicyException(
                MigrationViolationCode.INVALID_NAME,
                "migration name must match db/migration/V<positive integer>_<lower snake case>.sql: " + displayPath);
    }

    private static String safeDisplay(String path) { // 路径脱敏显示，防止字符过长路径破坏日志排版
        if (path == null || path.isBlank()) return "<blank>";
        String normalized = normalize(path).replace("[\\p{Ctrl}]", "?"); // 过滤控制字符
        int separator = normalized.lastIndexOf("/");
        String fileName = separator >= 0 ? normalized.substring(separator + 1) : normalized;
        if (fileName.isBlank()) fileName = "<unnamed>";
        return fileName.length() <= 128 ? fileName : fileName.substring(0, 128); // 限制文件长度

    }
}

