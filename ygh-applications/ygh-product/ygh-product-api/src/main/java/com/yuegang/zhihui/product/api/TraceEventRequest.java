package com.yuegang.zhihui.product.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 产品溯源事件请求记录类（使用 Java Record 特性）
 */
public record TraceEventRequest(
        @NotBlank String type, // 溯源事件类型，要求不能为空，且长度必须大于0（既不能全是空格）
        @Size(max = 200) String location, // 事件发生的地理位置或场所名称，限制该字符串的最大长度为200个字符
        @NotNull OffsetDateTime occurredAt, // 事件发生的精确日期和时间（包含时区偏移信息）
        Map<String, Object> details // 事件的扩展详细信息，以 JSON 风格的键值对姓氏存储
) { } // 记录类定义结束