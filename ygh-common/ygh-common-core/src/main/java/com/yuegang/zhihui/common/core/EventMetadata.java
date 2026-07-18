package com.yuegang.zhihui.common.core;

import java.time.OffsetDateTime;



/*传输适配器和业务不可变事件头*/
public record EventMetadata (
                                String eventId,
                                String eventType,
                                int eventVersion,
                                OffsetDateTime occurredAt,
                                String traceId, // 触发事件的请求链路
                                String producer, // 产生事件的服务标识
                                String bussinessKey // 业务聚合根ID

){
    private static final String SAFE_ID = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}";
    private static final String SAFE_TRACE = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}";
    private static final String SAFE_PRODUCER = "[a-z0-9][a-z0-9]{0,63}";

    public EventMetadata{
        requireMatch(eventId,"eventId",SAFE_ID);
        if (eventType == null || !eventType.matches("[A-Z][A-Z0-9_]{0,63}")){
            throw new IllegalArgumentException("eventType must be an uppercase stable code");

        }
        if (eventVersion < 1){
            throw new IllegalArgumentException("eventVersion must be a uppercase stable code");
        }
        if (occurredAt == null){
            throw new IllegalArgumentException("occurredAt must not be null");
        }
        requireMatch(traceId,"traceId",SAFE_TRACE);
        requireMatch(producer,"producer",SAFE_PRODUCER);
        requireMatch(bussinessKey,"bussinessKey",SAFE_ID);

    }

    private  static void requireMatch(String value, String fieldName, String pattern){
        if (value == null || !value.matches(pattern)){
            throw new IllegalArgumentException(fieldName + " is malformed " );

        }

    }


}
