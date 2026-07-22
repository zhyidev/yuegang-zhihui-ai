package com.yuegang.zhihui;

public enum MessageClaimStatus { // 认领操作的三种可能枚举
    CLAIMED, // 成功认领：你是当前唯一合法的执行者
    DUPLICATE, // 重复消息：该消息之前有别人成功处理
    IN_PROGRESS // 正在进行：有人正在处理，并租约有人在解析
}
