package com.yuegang.zhihui.auth.domain;

import java.util.Set;

/**
 * 权限提供者
 */
public interface AuthorityProvider { // 定义权限提供者接口，用于获取用户角色和权限
    Authorities find(long userId); // 根据用户 ID 查找权限信息

    record Authorities(Set<String> roles, Set<String> permissions) { // 定义 Authorities Record
        public Authorities { // 构造函数，创建不可变副本
            roles = Set.copyOf(roles);
            permissions = Set.copyOf(permissions);
        }
    }
}