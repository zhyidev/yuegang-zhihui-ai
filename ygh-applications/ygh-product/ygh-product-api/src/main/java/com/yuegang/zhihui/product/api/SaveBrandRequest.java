package com.yuegang.zhihui.product.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建或更新品牌的请求 DTO
 */
public record SaveBrandRequest(
    @NotBlank @Size(max = 64) String code, // 编码：不能为空，最大长度 64
    @NotBlank @Size(max = 100) String name, // 名称：不能为空，最大长度 100
    @Size(max = 512) String logoUrl // Logo URL：可选，最大长度 512
) {
}
