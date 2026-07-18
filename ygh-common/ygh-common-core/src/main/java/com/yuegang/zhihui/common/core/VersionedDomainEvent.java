package com.yuegang.zhihui.common.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 领域事件契约的默认类型安全实现*/
public final class VersionedDomainEvent<T> implements DomainEvent<T> {

    private final EventMetadata metadata;
    private final T payload;

    public VersionedDomainEvent(EventMetadata metadata, T payload){
        if(metadata == null) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        if(payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        this.metadata = metadata;
        this.payload = payload;
    }

    public static <T extends ImmutableEventPayload> VersionedDomainEvent<T> of(
            EventMetadata metadata,
            T payload
    ){
        return new VersionedDomainEvent<>(metadata,payload);
    }

    public static VersionedDomainEvent<Map<String, Object>> ofMap(
            EventMetadata metadata,
            Map<String, Object> payload
    ){
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        return new VersionedDomainEvent<>(metadata,immutableMap(payload));
    }



    @Override
    public EventMetadata metadata() { // 实现接口方法

        return metadata;
    }

    @Override
    public T payload() {
        return payload;
    }

    private static Map<String, Object> immutableMap(Map<String, Object> source) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>(source.size());
        source.forEach((key, value) -> copy.put(key, value));
        return Collections.unmodifiableMap(copy);
    }

    private static Object immutableNestedValue(Object value){
        if(value instanceof Map<?,?>map){
            LinkedHashMap<Object,Object> copy = new LinkedHashMap<>(map.size());
            map.forEach((key,item)->copy.put(key, immutableNestedValue(item)));
            return Collections.unmodifiableMap(copy);
        }
        if (value != null && value.getClass().isArray()) {
            throw  new IllegalArgumentException("array are not supported in map event payload");
        }
        return value;
    }

}
