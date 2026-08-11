package com.yuegang.zhihui.product.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 创建生产批次的请求 DTO
 */
public record SaveProductBatchRequest(
    @NotBlank @Size(max = 64) String batchNo, // 批次号：不能为空，上限 64
    @Size(max = 200) String origin, // 产地：上限 200 字符
    @Size(max = 512) String proofUrl, // 证明文件 URL：上限 512 字符
    LocalDate producedOn, // 生产日期
    LocalDate expiresOn, // 到期日期
    @Size(max = 2000) String traceDescription // 溯源详细描述，上限 2000 字符
) {
}
