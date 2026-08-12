package com.yuegang.zhihui.common.mq;

@FunctionalInterface // 函数式接口标识
public interface MessageBusinessOperation { // 消息业务操作接口
    void execute() throws Exception; // 执行消息业务操作，可能抛出异常
}
