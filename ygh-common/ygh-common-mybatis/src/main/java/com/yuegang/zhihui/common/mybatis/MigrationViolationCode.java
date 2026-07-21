package com.yuegang.zhihui.common.mybatis;

/* 由代码库和数据库迁移发出的稳定违规代码 */
public enum MigrationViolationCode {
    INVALID_NAME,                   //命名不合规
    UNDO_SCRIPT_FORBIDDEN,          // 禁止 Undo 脚本
    REPEATABLE_SCRIPT_FORBIDDEN,    // 禁止重复执行脚本
    DUPLICATE_VERSION,              // 重复版本号
    DUPLICATE_RESOURCE,             // 重复资源
    OUT_OF_ORDER_VERSION,           // 版本号顺序错误
    UNSAFE_CONFIGURATION,           // 不安全的配置（如禁用验证、允许清理、忽略迁移等）
    UNSUPPORTED_MIGRATION_TYPE,     // 不支持的迁移类型（如 SQL、Java、Spring 等）
    HISTORY_VALIDATION_FAILED       // 历史校验失败

}
