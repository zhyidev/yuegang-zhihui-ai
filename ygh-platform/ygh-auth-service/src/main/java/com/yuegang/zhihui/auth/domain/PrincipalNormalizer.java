package com.yuegang.zhihui.auth.domain;

import java.util.Locale;

/**
 * 凭证规范化工具
 */
public final class PrincipalNormalizer { // 登录凭证（如用户名、邮箱）的标准化处理器
    private PrincipalNormalizer() {
    } // 禁止实例化

    public static String normalize(String principal) { // 执行规范化
        if (principal == null) throw new IllegalArgumentException("principal must not be null");
        // 去除前后空格并统一转为小写，（由于为了区分向量和常量还有当前凭证，所以当前凭证全部进行小写操作）
        String normalized = principal.strip().toLowerCase(Locale.ROOT);
        // 校验安全性：不能为空，长度不能过大、不能包含控制字符
        if (normalized.isBlank() || normalized.codePointCount(0, normalized.length()) > 190
                || normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("principal is unsafe");
        }
        return normalized;
    }
}
