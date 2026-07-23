package com.yuegang.zhihui.common.mq;

public final class NonRetryableMessageException extends MessageHandlingException {
    public NonRetryableMessageException(String failureCode) {
        super(failureCode); // 一旦抛出此类异常，消息直接传入死信，不会尝试下次投递（节约资源）
    }
}
