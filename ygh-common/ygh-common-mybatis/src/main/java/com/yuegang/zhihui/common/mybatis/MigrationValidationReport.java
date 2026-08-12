package com.yuegang.zhihui.common.mybatis;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 不可变的，排序稳定的报告，适合作为 CI/CD 输出。
 *
 */
public record MigrationValidationReport(List<MigrationViolation> migrationViolationlist) { //包含违规清单

    private static final Comparator<MigrationViolation> ORDER =  // 排序规则：先代码后路径
            Comparator.comparing(MigrationViolation::code)
                    .thenComparing(MigrationViolation::resourcePath);

    public MigrationValidationReport { // 构造函数
        Objects.requireNonNull(migrationViolationlist, "migrationViolation must not be null");
        migrationViolationlist = migrationViolationlist.stream().sorted(ORDER).toList(); // 执行排序并转化为不可变的列表


    }

    public boolean valid() {
        return migrationViolationlist.isEmpty();
    }

    public void throwIfInvalid() { //如果不合法，抛出包含首个错误信息的异常
        if (!valid()) {
            var firstViolation = migrationViolationlist.getFirst();
            throw new MigrationPolicyException(firstViolation.code(), "migration policy failed with violation: "
                    + migrationViolationlist.size() + " violation(s), first violation: " + firstViolation.message());
        }

    }

}
