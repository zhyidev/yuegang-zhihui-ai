package com.yuegang.zhihui.system.api;

import java.util.Set;

public record RoleView(String id, String code, String name, boolean enabled, long version,
                       Set<String> permissions) { // 包含主键ID、角色编码、名称、是否启用、版本号和权限集合
    /**
     * 辅助构造函数：不带权限列表的初始化
     */
    public RoleView(String id, String code, String name, boolean enabled, long version) { // 构造方法开始
        this(id, code, name, enabled, version, Set.of()); // 调用主构造函数，权限集传入空集
    }

    /**
     * 紧凑构造函数：防御性拷贝
     */
    public RoleView { // 构造逻辑开始
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions); // 如果权限集为空则设为空集，否则创建不可变副本
    }
}
