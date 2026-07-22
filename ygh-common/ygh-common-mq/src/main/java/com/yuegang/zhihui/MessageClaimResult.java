package com.yuegang.zhihui;


import java.util.Objects;
import java.util.Optional;

public record MessageClaimResult(MessageClaimStatus status, Optional<MessageProcessingClaim> claim) { // 认领请求返回结果记录
    public MessageClaimResult { // 执行一致性校验
        Objects.requireNonNull(status, "status must not be null"); // 状态不能为空
        Objects.requireNonNull(claim, "claim must not be null"); // 认领信息不能为空
        if ((status == MessageClaimStatus.CLAIMED) != claim.isPresent()) { // 规则：仅当状态未已认领时，必须包含凭证对象
            throw new IllegalArgumentException("only CLAIMED may contain a claim"); // 逻辑冲突报错
        }

    }

    public static MessageClaimResult claimed(MessageProcessingClaim claim) { // 成功认领的工厂方法
        return new MessageClaimResult(MessageClaimStatus.CLAIMED, Optional.of(claim)); // 返回已认领状态和凭证对象

    }

    public static MessageClaimResult duplicate() { // 重写消费方法
        return new MessageClaimResult(MessageClaimStatus.DUPLICATE, Optional.empty()); // 返回重复消费状态和空凭证对象
    }

    public static MessageClaimResult inProgress() { // 别人处理的工厂方法
        return new MessageClaimResult(MessageClaimStatus.IN_PROGRESS, Optional.empty());
    }
}
