package com.yuegang.zhihui.common.mq;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.regex.Pattern;

/**
 * 处理权凭证类
 * 包含持有者处理信息认领凭证，用于防止已超时的消费者线程，误写其他节点尝试结果
 */
public record MessageProcessingClaim(
        String consumerGroup, // 组 ID
        String eventId, // 消息唯一 ID
        @JsonIgnore String owner // 持有者令牌（JSON序列化时忽略以防泄露）
) { // 凭证纪录类
    private static final Pattern GROUP = Pattern.compile("[a-z0-9][a-z0-9-]{0,63}"); // 组名正则规则
    private static final Pattern OWNER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{32,127}"); // 持有者 ID（令牌） 正则规则

    public MessageProcessingClaim { // 紧凑构造函数逻辑
        if (consumerGroup == null || !GROUP.matcher(consumerGroup).matches()) { // 校验组名格式
            throw new IllegalArgumentException("consumerGroup is malformed"); // 不符合抛异常
        }

        if (eventId == null || eventId.isBlank() || eventId.length() > 128) { // 校验消息 ID
            throw new IllegalArgumentException("eventId must not be null, blank or exceed 128 characters"); // 不符合抛异常
        }
        if (owner == null || !OWNER.matcher(owner).matches()) { // 校验持有者 ID（令牌）格式
            throw new IllegalArgumentException("claim owner is malformed"); // 不符合抛异常
        }

    }

    @Override
    public String toString() { // 重写toString 实现脱敏
        return "MessageProcessingClaim[consumerGroup=" + consumerGroup
                + ",eventId =" + eventId + ", owner=[REDACTED]]";// 在日志中隐藏真是令牌

    }
}
