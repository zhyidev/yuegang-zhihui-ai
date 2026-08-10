package com.yuegang.zhihui.product.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 产品综合详情显示模型
 */
public record ProductView(String spuId, // 标准产品单位（SPU）ID
                          String skuId, // 最小存货单位（SKU）ID
                          String categoryId, // 所属分类 ID
                          String brandId, // 所属品牌 ID
                          String name, // 产品名称
                          String skuCode, // SKU 外部编码
                          BigDecimal price, // 产品单价
                          String currency, // 货币代码（如 CNY、USD）
                          ProductStatus status, // 当前产品状态（枚举）
                          List<String> images, // 产品图片列表（URL 集合）
                          String traceabilityCode, // 全球溯源码或内部溯源码
                          long version, // 乐观锁版本号
                          Map<String, String> specifications // 产品规格参数映射（如：颜色：红色，尺寸：XL）
) {
    // 辅助构造函数，若干不提供规格参数，则默认为空 Map
    public ProductView(String spuId, String skuId, String categoryId, String brandId, String name, String skuCode, BigDecimal price, String currency, ProductStatus status, List<String> images, String traceabilityCode, long version) {
        this(spuId, skuId, categoryId, brandId, name, skuCode, price, currency, status, images, traceabilityCode, version, Map.of());
    }

    // 紧凑构造函数：确保 specifications 永远不为 null 且不可变
    public ProductView {
        specifications = specifications == null ? Map.of() : Map.copyOf(specifications);
    }

}