package com.yuegang.zhihui.common.core;

import java.time.OffsetDateTime;

public record EventMetadata(//定义事件元数据记录
        String eventId, // 事件唯一标识
        String eventType, // 事件具体类型
        int eventVersion, // 事件版本
        OffsetDateTime occurredAt, // 事件实际发生的时间
        String traceId, // 触发事件的请求链路 ID
        String producer, // 产生事件的服务标识
        String businessKey // 业务聚合根 ID（如订单号）

) {
    private static final String SAFE_ID = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}"; // ID 安全正则表达式
    private static final String SAFE_TRACE = "[A-Za-z0-9][A-Za-z0-9._-]{0,127}"; // 链路 ID 安全正则
    private static final String SAFE_PRODUCER = "[a-z0-9][a-z0-9]{0,63}"; // 生产者名称安全正则

    public EventMetadata { // 紧凑构造函数
        requireMatch(eventId, "eventId", SAFE_ID); // 校验事件 ID 格式
        if (eventType == null || !eventType.matches("[A-Z][A-Z0-9_]{0,63}")) { // 校验事件类型必须是大写蛇形命名
            throw new IllegalArgumentException("eventType must be an uppercase stable code"); // 否则抛出异常
        }
        if (eventVersion < 1) { // 校验版本必须大于等于 1
            throw new IllegalArgumentException("eventVersion must be an uppercase stable code"); // 否则抛出异常
        }
        if (occurredAt == null) {  // 校验发生时间不能为空
            throw new IllegalArgumentException("occurredAt must not be null"); // 否则抛出异常
        }
        requireMatch(traceId,  "traceId", SAFE_TRACE); // 校验追踪 ID 格式
        requireMatch(producer,  "producer", SAFE_PRODUCER); // 校验生产标识格式
        requireMatch(businessKey,  "businessKey", SAFE_ID); // 校验业务主键格式
    }
    private static void requireMatch(String value, String fieldName, String pattern) {  // 正则匹配辅助方法
        if (value == null || !value.matches(pattern)) {  // 若值为空或不匹配正则
            throw new IllegalArgumentException(fieldName + "is malformed");  // 抛出格式错误异常
        }
    }

}
