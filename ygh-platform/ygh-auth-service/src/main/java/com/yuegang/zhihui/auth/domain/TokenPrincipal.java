package com.yuegang.zhihui.auth.domain;

import java.util.Objects;
import java.util.Set;

/**
 * 令牌主体信息
 */
public record TokenPrincipal(long accountId, long userId, Set<String> roles,
                             Set<String> permissions) { // 封装在 Token 中的核心权限身份主体 Record

    public TokenPrincipal { // 校验构造函数
        if (accountId <= 0 || userId <= 0) throw new IllegalArgumentException("token identifiers must be positive");
        roles = Set.copyOf(Objects.requireNonNull(roles, "roles must not be null")); // 创建不可变副本
        permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions must not be null"));
        // 校验权限声明的内容安全性，防止注入攻击和内存溢出攻击
        validateClaims(roles, "[A-Z][A-Z0-9_: -]{0,127}"); // 校验角色正则
        validateClaims(permissions, "[A-Za-z][A-Za-z0-9:_-]{0,127}"); // 校验权限正则
    }

    private static void validateClaims(Set<String> claims, String safePattern) { // 执行权限集合内容的细节校验
        if (claims.size() > 128) throw new IllegalArgumentException("claim count exceeds limit"); // 数量限制
        int encodedLength = 0;
        for (String claim : claims) {
            if (claim == null || !claim.matches(safePattern)) { // 校验单个权限项的格式
                throw new IllegalArgumentException("claim contains unsafe value");
            }
            // 计算编码后的总长度，确保在放入 JWT 时不会因 Header 过大而被网关拦截
            encodedLength += claim.length() + (encodedLength == 0 ? 0 : 1);
        }
        if (encodedLength > 4096) throw new IllegalArgumentException("encoded claims exceed limit"); // 总长度限制（4KB）
    }
}
