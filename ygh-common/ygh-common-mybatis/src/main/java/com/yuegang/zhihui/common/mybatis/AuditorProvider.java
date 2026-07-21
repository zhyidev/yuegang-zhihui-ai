package com.yuegang.zhihui.common.mybatis;

/** 提供持久化审计字段中记录的稳定外部标识符 */
@FunctionalInterface
public interface AuditorProvider {

    String SYSTEM_AUDITOR = "SYSTEM";

    String currentAuditor();

    static AuditorProvider system() {
        return () -> SYSTEM_AUDITOR;
    }
}
