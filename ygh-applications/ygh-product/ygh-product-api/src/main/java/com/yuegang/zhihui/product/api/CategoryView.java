package com.yuegang.zhihui.product.api;

/**
 * 产品分类显示模型
 */
public record CategoryView(
        String id,          // 分类唯一标识 ID
        String parentId,    // 父级分类 ID（若为顶层分类则为空）
        String code,        // 分类编码
        String name,        // 分类名称
        int sortOrder,      // 排序序号，用于前端展示排序
        boolean enabled,    // 该分类是否激活启用
        long version        // 乐观锁版本号
) {
}