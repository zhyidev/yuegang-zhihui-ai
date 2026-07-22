package com.yuegang.zhihui.common.mybatis;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.InfoOutput;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class YghFlywayMigrationStrategy implements FlywayMigrationStrategy {

    private final FlywayMigrationPolicy migrationPolicy;
    private final FlywayConfigurationGuard configurationGuard;
    private final FlywayHistoryValidator historyValidator;

    public YghFlywayMigrationStrategy(
            FlywayMigrationPolicy migrationPolicy,
            FlywayConfigurationGuard configurationGuard,
            FlywayHistoryValidator historyValidator
    ){
        this.migrationPolicy = Objects.requireNonNull(migrationPolicy,"migrationPolicy cannot be null");
        this.configurationGuard = Objects.requireNonNull(configurationGuard,"configurationGuard cannot be null");;
        this.historyValidator = Objects.requireNonNull(historyValidator,"historyValidator cannot be null");;
    }

    @Override
    public void migrate(Flyway flyway){
        Objects.requireNonNull(flyway,"flyway cannot be null");
        configurationGuard.validateOrThrow(flyway.getConfiguration());

        var info = flyway.info().getInfoResult();
        List<String> allResources = new ArrayList<>();
        List<String> notAppliedResources =  new ArrayList<>();
        long highestAppliedVersion = 0L;

        for (InfoOutput migration : info.migrations){
            if (!isSqlMigration(migration)){
                throw new MigrationPolicyException(MigrationViolationCode.UNSUPPORTED_MINRATION_TYPE, "only SQL migrations are supported");
            }
            if (isRepeatable(migration)){
                throw new MigrationPolicyException(MigrationViolationCode.REPEATABLE_SCRIPT_FORBIDDEN, "repeatable migration forbidden");
            }
            if (isUndo(migration)){
                throw new MigrationPolicyException(MigrationViolationCode.UNDO_SCRIPT_FORBIDDEN, "undo migration if forbidden");
            }
            if (migration.filepath != null && migration.filepath.endsWith(".sql")){
                String policyPath = configurationGuard.toPolicyResoourcePath(flyway.getConfiguration(), migration.filepath);
                allResources.add(policyPath);
                if (migration.installedOnUTC == null){
                    notAppliedResources.add(policyPath);
                }
            }
            String resolvedVersion = resolvedVersion(migration);
            if (resolvedVersion != null && isApplied(migration)){
                highestAppliedVersion = Math.max(highestAppliedVersion,parseAppliedVersion(resolvedVersion));
            }
        }
        migrationPolicy.validate(allResources).throwIfInvalid();
        for (InfoOutput migration : info.migrations){
            String resolvedVersion = resolvedVersion(migration);
            if (resolvedVersion != null && !isApplied(migration)
                    && parseAppliedVersion(resolvedVersion) <= highestAppliedVersion){
                throw new MigrationPolicyException(MigrationViolationCode.OUT_OF_ORDER_VERSION, "resolved migration version must be greater than applied  history");
            }
        }
        migrationPolicy.validateNewMigrations(notAppliedResources, highestAppliedVersion).throwIfInvalid();
        flyway.migrate();
        historyValidator.validateOrThrow(flyway);
    }

    private static boolean isRepeatable(InfoOutput migration){
        boolean repeatableCategory = migration.category != null &&
                migration.category.toLowerCase().contains("repeatable");
        boolean SqlWithoutVersion = resolvedVersion(migration) == null &&
                migration.filepath != null && migration.filepath.toLowerCase().endsWith(".sql");
        return repeatableCategory || SqlWithoutVersion;
    }

    private static boolean isUndo(InfoOutput migration){
        if (migration.filepath == null) return false;
        String normalized = migration.filepath.replace('\\','/');
        int separator =  normalized.lastIndexOf('/');
        String fileName = separator >= 0 ? normalized.substring(separator + 1) : normalized;
        return fileName.startsWith("U");

    }

    private static boolean isSqlMigration(InfoOutput migration) {
        return migration.type != null && migration.type.toLowerCase().contains("sql");
    }

    private static long parseAppliedVersion(String version){
        try { return Long.parseLong(version); }
        catch (NumberFormatException exception){ throw  new MigrationPolicyException(MigrationViolationCode.INVALID_NAME,"applied migration uses unsupported version format"); }
    }

    private static String resolvedVersion(InfoOutput migration) {
        if (migration.type != null && !migration.rawVersion.isBlank()) return migration.rawVersion;
        return migration.version == null || migration.version.isBlank() ? migration.rawVersion : migration.version;

    }

    public static boolean isApplied(InfoOutput migration) {
        if (migration.installedOnUTC != null && !migration.installedOnUTC.isBlank()) return true;
        return migration.state != null &&
                migration.state.toLowerCase().contains("success");
    }
}
