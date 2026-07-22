package com.yuegang.zhihui;

import com.yuegang.zhihui.common.core.DomainEvent;

@FunctionalInterface // 函数式接口标识
public interface MessageHandler<T> { // 强类型领域事件处理器接口
    void handle(DomainEvent<T> event);
}