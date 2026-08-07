package com.yuegang.zhihui.product.api;

/**
 * 品牌信息展示模块
 */
public record BrandView(
        String id, // 品牌唯一标识 ID
        String code, // 品牌编码（通常为唯一字符串）
        String name, // 品牌显示名称
        String logoUrl, // 品牌图标的 URL 地址
        boolean enabled, // 品牌是否启用状态
        long version // 乐观锁版本号，用于并发控制
) {
}