package com.yuegang.zhihui.product.api;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 创建新产品请求 DTO
 */
public record SaveProductRequest(
    @NotBlank String categoryId, // 分类 ID：不能为空
    String brandId, // 品牌 ID：可选
    @NotBlank @Size(max = 200) String name, // 产品名称：不能为空，最大 200
    @NotBlank @Size(max = 64) String skuCode, // SKU 编码：不能为空，最大 64
    @NotNull @DecimalMin("0.00")
    @Digits(integer = 16, fraction = 2) BigDecimal price, // 价格：必填，最小 0.00，支持 16 位整数和 2 位小数
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency, // 货币：不能为空，必须是 3 位大写字母（如 CNY）
    @NotNull @Size(max = 20) List<@Size(max = 512) String> images, // 图片列表：不为空，最多 20 张，每张 URL 限 512 字符
    @Size(max = 128) String traceabilityCode, // 溯源码，最大 128
    @PositiveOrZero long version, // 乐观锁版本号，必须大于等于 0
    @Size(max = 30) Map<@NotBlank @Size(max = 64) String, @NotBlank @Size(max = 200) String> specifications
// 规格参数，最多 30 组
) {
    // 辅助构造函数，初始化时若无规格参数测试为空 Map
    public SaveProductRequest(
        String categoryId, String brandId, String name, String skuCode,
        BigDecimal price, String currency, List<String> images,
        String traceabilityCode, long version) {
        this(categoryId, brandId, name, skuCode, price, currency, images, traceabilityCode, version, Map.of());
    }

    // 紧凑构造函数：保证参数安全性和不可变性
    public SaveProductRequest {
        specifications = specifications == null ? Map.of() : Map.copyOf(specifications);
    }
}
