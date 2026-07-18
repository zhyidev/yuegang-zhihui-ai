package com.yuegang.zhihui.common.core;

public interface DomainEvent<T> { // 领域时间契约

    EventMetadata metadata();

    T payload();

    default String eventId(){
        return metadata().eventId();
    }


}
