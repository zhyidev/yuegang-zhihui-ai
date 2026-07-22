package com.yuegang.zhihui.common.mybatis;

public record MigrationDescriptor (String resourcePath, long version, String description) {

    public MigrationDescriptor{
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("resource path must not be blank");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
    }
}
