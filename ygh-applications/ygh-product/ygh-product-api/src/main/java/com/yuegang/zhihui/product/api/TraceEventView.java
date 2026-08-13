package com.yuegang.zhihui.product.api;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 产品生命周期中的单个溯源/物流事件视图模型
 */
public record TraceEventView(
    String id,              // 事件唯一 ID
    String skuId,           // 关联的 SKU ID
    String type,            // 事件类型（如：入库、检验、发货）
    String location,        // 事件发生的地理位置
    OffsetDateTime occurredAt, // 事件发生的精确时间（包含时区）
    Map<String, Object> details // 事件的动态详细信息（JSON 键值对）
) {
}
