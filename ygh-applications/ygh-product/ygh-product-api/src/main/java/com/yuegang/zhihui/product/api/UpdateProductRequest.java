package com.yuegang.zhihui.product.api;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 更新现有的产品信息的请求 DTO
 */
public record UpdateProductRequest(
    @NotBlank String categoryId, // 分类 ID
    String brandId, // 所属品牌 ID
    @NotBlank @Size(max = 200) String name, // 产品名称：限 200 字符
    @Size(max = 5000) String description, // 产品详细描述文案，限 5000 字符
    @NotNull @DecimalMin("0.00")
    @Digits(integer = 16, fraction = 2) BigDecimal price, // 单价校验
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency, // 货币校验
    @NotNull @Size(max = 20) List<@Size(max = 512) String> images, // 图片库校验
    @Size(max = 128) String traceabilityCode, // 溯源码校验
    @PositiveOrZero long version, // 更新必须携带当前的版本号 乐观锁版本号，必须大于等于 0
    @Size(max = 30) Map<@NotBlank @Size(max = 64) String, @NotBlank @Size(max = 200) String> specifications// 规格参数校验
) {
    // 辅助构造函数：用于简化初始化调用
    public UpdateProductRequest(
        String categoryId, String brandId, String name, String description,
        BigDecimal price, String currency, List<String> images,
        String traceabilityCode, long version) {
        this(categoryId, brandId, name, description, price, currency, images, traceabilityCode, version, Map.of());
    }

    // 紧凑构造函数：防御性拷贝和空值处理
    public UpdateProductRequest {
        specifications = specifications == null ? Map.of() : Map.copyOf(specifications);
    }
}
