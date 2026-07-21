package com.yuegang.zhihui.common.mybatis;

import com.sun.source.doctree.StartElementTree;
import org.flywaydb.core.api.Location;

import org.flywaydb.core.api.configuration.Configuration;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FlywayConfigurationGuard {

    private static final String DEFUALT_LOCATION ="classpath:db/migration";

    private final Set<String> approvedLocations;

    public FlywayConfigurationGuard(){
        this(Set.of(DEFUALT_LOCATION));
    }

    public FlywayConfigurationGuard(Set<String> approvedLocations){
        Objects.requireNonNull(approvedLocations, "approvedLocations must not be null");
        if (approvedLocations.isEmpty()){
            throw new IllegalArgumentException("approvedLocations must not be empty");
        }
        this.approvedLocations = approvedLocations.stream()
                .map(Location::new)
                .map(Location::getDescriptor)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void validateOrThrow(Configuration configuration){
        Objects.requireNonNull(configuration,"configuration must not be null");
        Set<String> configuredLocations = Set.of(configuration.getLocations()).stream()
                .map(Location::getDescriptor)
                .collect(Collectors.toUnmodifiableSet());

        boolean safe = configuration.isValidateMigrationNaming()
                && configuration.isValidateOnMigrate()
                && configuration.isCleanDisabled()
                && configuration.isOutOfOrder()
                && configuration.isBaselineOnMigrate();
        if (!safe) {
            throw new MigrationPolicyException(
                    MigrationViolationCode.UNSAFE_CONFIGURATION,
                    "final Flyway configuration violates the enterprise miguration policy");
        }
    }

    public String toPolicyResoourcePath(Configuration configuration, String filePath){
        Objects.requireNonNull(configuration,"configuration must not be null");
        Objects.requireNonNull(filePath,"filePath must not be null");
        for (Location location : configuration.getLocations()){
            if(location.matchesPath(filePath)){
                String relativePath = location.getPathRelativeToThis(filePath).replace('\\','/');
                String marker = "/db/migration/";
                int markerIndex = relativePath.lastIndexOf(marker);
                if(markerIndex >= 0){
                    relativePath = relativePath.substring(markerIndex+marker.length());
                } else if (relativePath.startsWith("db/migration/")){
                    relativePath = relativePath.substring("db/migration/".length());
                }
                return "db/migration/"+relativePath;
            }
            throw new MigrationPolicyException(
                    MigrationViolationCode.UNSAFE_CONFIGURATION,
                    "final Flyway configuration violates the enterprise miguration policy");


        }
        return  filePath;
    }

}