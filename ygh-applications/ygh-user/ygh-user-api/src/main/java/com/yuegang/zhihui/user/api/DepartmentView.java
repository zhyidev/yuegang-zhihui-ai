package com.yuegang.zhihui.user.api;

public record DepartmentView(
        String id, // 属性: 部门唯一主键ID
        String parentId, // 属性: 上级部门ID
        String code, // 属性: 部门业务编码
        String name, // 属性: 部门名称
        int sortOrder, // 属性: 排序值
        boolean enabled, // 属性: 状态（启用/禁用）
        long version // 属性: 乐观锁版本号
) {
} // 类定义结束
