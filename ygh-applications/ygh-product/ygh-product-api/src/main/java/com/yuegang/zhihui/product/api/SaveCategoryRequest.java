package com.yuegang.zhihui.product.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建或更新分类的请求 DTO
 */
public record SaveCategoryRequest(
        String parentId, // 父级分类 ID
        @NotBlank @Size(max = 64) String code, // 编码：不能为空，长度上限 64
        @NotBlank @Size(max = 100) String name, // 名称：不能为空，长度上限 100
        int sortOrder // 排序权重值
) {
}
