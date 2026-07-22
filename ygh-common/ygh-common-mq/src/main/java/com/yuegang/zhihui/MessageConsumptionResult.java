package com.yuegang.zhihui;

public enum MessageConsumptionResult { // 整个消息生命周期在应用层的反馈结果枚举
    ACKNOWLEDGED, // 处理成功：通知中间件删除消息
    DUPLICATE,// 业务重复：通知中间件删除消息（防止重传）
    RETRY,// 需要重试：同志中间件稍后重新传递
    DEAD_LETTERED // 传入死信，业务已经彻底失败，不再重试
}
