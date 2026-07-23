package com.yuegang.zhihui.common.mq;

import java.time.Duration;

/**
 * 具有持久化能力的消费者幂等边界协议。
 * 实现类必须在每次状态变更时严格对比持有令牌
 * 业务操作混合状态扭转到 SUCCESSES 必须通一个本地数据库食物中提交
 * 如果租约凭证已过期或失效，严格执行业务操作并返回 false
 * 如果业务代码抛出异常，整个本地事物必须回滚，防止业务已执行但状态未标记成功
 */
public interface MessageConsumptionStore { // 定义存储契约
    MessageClaimResult claim(  // 尝试认领接口
                               String consumerGroup, // 组标识
                               String eventId, // 消息唯一标识令牌
                               String ownerToken, // 消费者令牌
                               Duration lease // 租约有效时间
    );

}
