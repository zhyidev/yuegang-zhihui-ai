package com.yuegang.zhihui.common.mybatis;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 针对单个服务拥有的数据库，实施代码库级别的"仅向前"迁移策略。
 * 数据库历史记录的物理检查仍由 Flyway 自行完成。
 */
public final class FlywayMigrationPolicy {

    private static final Pattern VERSIONED_MIGRATION = Pattern.compile(
            "^db/migration/v([1-9][0-9]*)__[a-z][a-z0-9]*(?:_[a-z0-9]+)*\\.sql$");

    public FlywayMigrationPolicy() {
    }

    public MigrationDescriptor parse(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath must not be null");
        String normalized = normalize(resourcePath);
        if (normalized.isBlank() || normalized.contains("../") || normalized.contains("/../")) {
            throw new MigrationPolicyException(
                    MigrationViolationCode.UNDO_SCRIPT_FORBIDDEN,
                    "undo migration is forbidden: " + safeDisplay(normalized));
        }
        if (normalized.startsWith("db/migration/R__")) {
            throw new MigrationPolicyException(
                    MigrationViolationCode.REPEATABLE_SCRIPT_FORBIDDEN,
                    "repeatable migration is forbidden: " + safeDisplay(normalized));
        }
        var matcher = VERSIONED_MIGRATION.matcher(normalized);
        if (!matcher.matches()) {
            throw invalid(normalized);
        }
        long version;
        try {
            version = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw invalid(normalized);
        }
        return new MigrationDescriptor(normalized, version, matcher.group(2));
    }

    public MigrationValidationReport validate(Collection<String> resourcePaths) {
        Objects.requireNonNull(resourcePaths, "resourcePaths must not be null");
        var violations = new ArrayList<MigrationViolation>();
        Set<String> seenPath = new HashSet<>();
        Map<Long, String> pathByVersion = new HashMap<>();

        for (String resourcePath : resourcePaths) {
            if (resourcePath == null) {
                violations.add(new MigrationViolation(MigrationViolationCode.INVALID_NAME, "<null>", "migration path must not be null"));
            }

            String normalized = normalize(resourcePath);
            if (!seenPath.add(normalized)) {
                String displayPath = safeDisplay(normalized);
                violations.add(new MigrationViolation(MigrationViolationCode.DUPLICATE_VERSION, displayPath, "migration resource is duplicated: " + displayPath));
                continue;
            }
            try {
                var descriptor = parse(normalized);
                String existingPath = pathByVersion.putIfAbsent(descriptor.version(), descriptor.resourcePath());
                if (existingPath != null) {
                    violations.add(new MigrationViolation(MigrationViolationCode.DUPLICATE_VERSION,
                            descriptor.resourcePath(), "version " + descriptor.version() + " is already used by " + existingPath));
                }
            } catch (MigrationPolicyException exception) {
                violations.add(new MigrationViolation(exception.getCode(), safeDisplay(normalized), exception.getMessage()));
            }
        }
        return new MigrationValidationReport(violations);
    }

    MigrationValidationReport validateNewMigrations(Collection<String> resourcePaths, long highestAppliedVersion) {
        if (highestAppliedVersion < 0) {
            throw new IllegalArgumentException("highestAppliedVersion must not be negative");
        }

        var violations = new ArrayList<>(validate(resourcePaths).violations());

        for (String resourcePath : resourcePaths) {
            if (resourcePath == null) continue;
            try {
                var descriptor = parse(resourcePath);
                if (descriptor.version() <= highestAppliedVersion) {
                    violations.add(new MigrationViolation(
                            MigrationViolationCode.OUT_OF_ORDER_VERSION,
                            descriptor.resourcePath(),
                            "new version " + descriptor.version() + " must be greater than applied version " + highestAppliedVersion));
                }
            } catch (MigrationPolicyException ignored) {
            }
        }
        return new MigrationValidationReport(violations);
    }

    private static String normalize(String resourcePath) {
        return resourcePath.replace('\\', '/');
    }

    public static MigrationPolicyException invalid(String path) {
        String displayPath = safeDisplay(path);
        return new MigrationPolicyException(
                MigrationViolationCode.INVALID_NAME,
                "migration name must match db/migration/V<positive integer>__<lower_snake_case>.sql: " + displayPath);
    }

    private static String safeDisplay(String path) {
        if (path == null || path.isBlank()) return "<blank>";
        String normalized = normalize(path).replace("[\\p{Cntrl}]", "?");
        int separator = normalized.lastIndexOf('/');
        String fileName = separator >= 0 ? normalized.substring(separator + 1) : normalized;
        if (fileName.isBlank()) fileName = "<unnamed>";
        return fileName.length() <= 128 ? fileName : fileName.substring(0, 128);
    }
}