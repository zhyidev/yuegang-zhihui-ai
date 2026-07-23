package com.yuegang.zhihui.common.mq;

public final class RetryableMessageException extends MessageHandlingException { // 可重试的消息异常
    public RetryableMessageException(String failureCode) {
        super(failureCode); //抛出此异常表示目前时临时错误应当继续按退避算法重投
    }
}
