package com.yuegang.zhihui.product.api;

import java.time.LocalDate;

/**
 * 产品生产/溯源批次显示模型
 */
public record ProductBatchView(
        String id,          // 批次唯一标识 ID
        String skuId,       // 关联最小库存单位 (SKU) ID
        String batchNo,     // 生产批次号
        String origin,      // 产地信息
        String proofUrl,    // 相关证明文件或证书的 URL
        LocalDate producedOn,// 生产日期
        LocalDate expiresOn, // 过期日期
        String traceDescription // 批次溯源详细描述说明
) {
}