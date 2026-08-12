package com.yuegang.zhihui.product.api;

/**
 * 产生生命周期状态
 */
public enum ProductStatus {
    DRAFT, // 草稿：编辑中，不可售
    PUBLISHED, // 已发布：正常上架销售中
    OFF_SHELF, // 已下架：暂时停止销售
}
