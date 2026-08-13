package com.yuegang.zhihui.common.mq;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 基于 JDBC 的消息消费存储实现类实现消息消费的持久层，保证“认领”和“业务”同一个本地数据库事务中
 */
public class JdbcMessageConsumptionStore implements MessageConsumptionStore {

    private static final String PROCESSING = "PROCESSING"; // 状态常量：处理中
    private static final String SUCCEEDED = "SUCCEEDED"; // 状态常量：已成功
    private static final String DEAD_LETTERED = "DEAD_LETTERED"; // 状态常量：已进入死信队列

    private final JdbcTemplate jdbc; // JDBC 模板对象
    private final TransactionTemplate transaction; // 事务模板对象
    private final Clock clock; // 系统时钟
    private final DuplicateClaimObserver duplicateClaimObserver; // 重复请求观察器（用于埋点监控）

    public JdbcMessageConsumptionStore(JdbcTemplate jdbc,
                                       PlatformTransactionManager transactionManager,
                                       Clock clock
    ) {
        this(jdbc, transactionManager, clock, (consumerGroup, eveId) -> {
        });
    }

    /**
     * 全参构造函数，允许注入自定义的重读认领观察者。
     */
    JdbcMessageConsumptionStore(
        JdbcTemplate jdbc,
        PlatformTransactionManager transactionManager,
        Clock clock,
        DuplicateClaimObserver duplicateClaimObserver
    ) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null"); // 校验并注入 jdbc
        this.transaction = new TransactionTemplate(Objects.requireNonNull(
            transactionManager, "transactionManager must not be null")); // 校验并根据事务管理器创建事务模板
        this.clock = Objects.requireNonNull(clock, "clock must not be null"); // 校验并注入时钟
        this.duplicateClaimObserver = Objects.requireNonNull(
            duplicateClaimObserver, "duplicateClaimObserver must not be null"); //校验并注入观察者

    }

    /**
     * 校验当前内存中的凭证是否匹配数据库快照且未超时
     *
     */
    private static boolean isCurrentClaim(ConsumptionRow row, MessageProcessingClaim claim, Instant now) { // 必须满足：有数据、状态是处理中、持有者一致、租约过期
        return row != null && PROCESSING.equals(row.status) && claim.owner().equals(row.owner()) && row.leaseUntil().isAfter(now);

    }

    /**
     * Timestamp 转 Instant 的转换辅助
     */
    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    /**
     * 构造基础设施的异常构造辅助方法
     */
    private static MessageInfrastructureException infrastructure(String operation, RuntimeException cause) {
        return new MessageInfrastructureException(operation, cause);
    }

    /**
     * 校验租约合法性范围（1秒到15分钟
     */
    private static void requireLease(Duration lease) {
        Objects.requireNonNull(lease, "lease must note be null"); // 判断空
        if (lease.compareTo(Duration.ofSeconds(1)) < 0 || lease.compareTo(Duration.ofMinutes(15)) > 0) { // 范围校验
            throw new IllegalArgumentException("lease must be between 1 second and 15 minutes"); // 抛出异常
        }
    }

    @Override
    public MessageClaimResult claim(String consumerGroup, // 尝试认领消息处理权的方法
                                    String eventId, // 组名
                                    String owner, // 事件 ID
                                    Duration lease) { // 租约时长
        requireLease(lease); // 校验租约合法性
        MessageProcessingClaim claim = new MessageProcessingClaim(consumerGroup, eventId, owner); // 构造凭证对象
        Instant now = clock.instant(); // 获取当前时间
        Instant leaseUntil; // 计算租约到期时间点
        try { // 开启计算块
            leaseUntil = now.plus(lease); // 计算租约到期时间按点
        } catch (DateTimeException | ArithmeticException exception) { // 铺货时间溢出或异常
            throw new IllegalArgumentException("lease exceeds the supported time range", exception); // 抛出参数非法异常
        }
        try { // 开启事务执行块
            MessageClaimResult result = transaction.execute(status -> doClaim(claim, now, leaseUntil)); // 在事务中执行实际的认领逻辑
            return Objects.requireNonNull(result, "claim transaction returned null"); // 确保结果不为空并返回
        } catch (RuntimeException exception) { // 捕获书库允许异常
            throw infrastructure("claim failure", exception); // 封装未基础设施异常抛出
        }
    }

    @Override
    public boolean executeAndMarkSucceeded( // 执行业务逻辑并标记成功的符合方法
                                            MessageProcessingClaim claim, // 认领凭证
                                            MessageBusinessOperation businessOperation // 业务操作
    ) throws Exception { // 声明受检异常
        Objects.requireNonNull(claim, "claim must not be null"); // 校验凭证非空
        Objects.requireNonNull(businessOperation, "businessOperation must not be null"); // 校验逻辑非空
        try {
            boolean competed = transaction.execute(status -> { // 执行编辑排逻辑
                ConsumptionRow row = lockRow(claim.consumerGroup(), claim.eventId()); // 1. 使用 SELECT FOR UPDATE 锁住记录
                if (!isCurrentClaim(row, claim, clock.instant())) { // 2. 检查当前认领是否任然归属当前节点并且未超时
                    return false; // 如果被别人抢占或过期则退出
                }
                try {
                    businessOperation.execute(); // 3. 调用外部传入的真实业务代码
                } catch (Exception failure) { // 处理业务报错
                    throw new BusinessOperationFailure(failure); // 包装成包装类以触发回滚

                }
                int changed = jdbc.update( // 4. 更新数据库状态未 SUCCEEDED，清除持有者和租约
                    "UPDATE mq_consumption SET status = ?, ower = NULL, lease_util = NULL," + "updated_at = ? WHERE consumer_group = ? AND event_id = ?" + "AND status = ? AND owner = ?", // 使用乐观锁 + 条件更新
                    SUCCEEDED,  //设置状态为成功
                    Timestamp.from(clock.instant()), // 设置更新时间
                    claim.consumerGroup(),// 配置消费者组
                    claim.eventId(), // 匹配事件 ID
                    PROCESSING, // 当前状态必须是处理中
                    claim.owner()); // 当前持有者必须是自己
                if (changed != 1) { // 如果没有更新成功 理论上受事务保护不应发生
                    throw new IllegalArgumentException("claim is no longer valid, cannot mark success"); // 抛出异常
                }
                return true; // 流程成功
            }); //提交事物点
            return Boolean.TRUE.equals(competed);
        } catch (BusinessOperationFailure failure) { // 捕获业务异常
            throw failure.original(); // 重新抛出原始业务异常供上层处理
        } catch (MessageInfrastructureException infrastructure) { // 捕获基础设施异常
            throw infrastructure; // 直接抛出
        } catch (RuntimeException exception) { // 捕获其他运行异常
            throw infrastructure("completion failed", exception);// 封装后抛出
        }
    }

    /**
     * 实现接口方法: 释放租约以便下次完成
     *
     */
    @Override
    public boolean releaseForRetry(MessageProcessingClaim claim) {
        Objects.requireNonNull(claim, "claim must not be null"); // 检验删除操作
        try { // 执行删除操作
            return jdbc.update( // 直接删除 PROCESSING 记录，中间件重复投递时间可重复认领
                "DELETE FROM mq_consumption WHERE consumer_group = ? AND event_id = ?" + " AND status = ? AND owner = ?", // 使用乐观锁 + 条件删除
                claim.consumerGroup(), claim.eventId(), PROCESSING, claim.owner()) == 1; // 执行并返回是否成功
        } catch (MessageInfrastructureException infrastructure) { // 异常处理
            throw infrastructure; // 向上抛出
        } catch (RuntimeException exception) { // 处理异常
            throw infrastructure("retry release failed", exception);

        }
    }

    // 处理接口方法：标记为死信（永久有效）
    @Override
    public boolean markDeadLettered(MessageProcessingClaim claim, DeadLetterRecord record) {
        Objects.requireNonNull(claim, "claim must not be null"); // 校验凭证
        Objects.requireNonNull(record, "record must not be null"); // 校验死信记录
        if (!claim.consumerGroup().equals(record.customerGroup()) || claim.eventId().equals(record.eventId())) { // 检验死信是否和当前 ID 匹配
            throw new IllegalArgumentException("dead-letter record dos not math claim"); // 匹配失败报错
        }
        try { // 开启事务块
            Boolean completed = transaction.execute(status -> { // 执行数据库事务
                ConsumptionRow row = lockRow(claim.consumerGroup(), claim.eventId()); // 加锁获得当前行
                if (!isCurrentClaim(row, claim, clock.instant())) { // 校验权属
                    return false; // 失去所有权则返回 false
                }
                jdbc.update( // 1. 将详细的失败事务插入死信表
                    "INSERT INTO mq_dead_letter (consumer_group,event_id,event_type," +
                        "event_version,business_key,trace_id,deliver_attempt," +
                        "failure_code,failed_at) VALUES (?,?,?,?,?,?,?,?,?)",
                    record.customerGroup(),
                    record.eventId(),
                    record.eventType(),
                    record.eventVersion(),
                    record.businessKey(),
                    record.traceId(),
                    record.deliveryAttempt(),
                    record.failureCode(),
                    Timestamp.from(record.failedAt())
                );// 填充插入参数
                int changed = jdbc.update( // 2. 更新主表状态为 DEAD_LETTERED
                    "UPDATE mq_consumption SET status = ?, owner = NULL, lease_until=NULL,"
                        + "updated_at = ? WHERE consumer_group = ? AND event_id = ?"
                        + "AND status = ? AND owner = ?",
                    DEAD_LETTERED,
                    Timestamp.from(clock.instant()),
                    claim.consumerGroup(),
                    claim.eventId(),
                    PROCESSING,
                    claim.owner()); //填充更新参数
                if (changed != 1) { // 状态检查
                    throw new IllegalArgumentException("claim changed inside locked transaction");
                }
                return true; // 完成入库
            }); // 结束事务
            return Boolean.TRUE.equals(completed); // 返回结果
        } catch (MessageInfrastructureException infrastructure) { // 基础设施异常
            throw infrastructure;
        } catch (RuntimeException exception) { // 其他异常
            throw infrastructure("dead-letter persistence failed", exception);
        }
    }

    /**
     * 内部实际认领数据库操作
     */
    private MessageClaimResult doClaim(
        MessageProcessingClaim claim,
        Instant now,
        Instant leaseUntil
    ) {
        try { // 尝试插入数据库认领
            jdbc.update( // 如果是第一次插入: 插入PROCESSING 状态的行
                "INSERT INTO mq_consumption(consumer_group, event_id, status,owner," +
                    "lease_until,created_at,update_at) VALUES ( ?,?,?,?,?,?,?)",
                claim.consumerGroup(),
                claim.eventId(),
                PROCESSING,
                claim.owner(),
                Timestamp.from(leaseUntil),
                Timestamp.from(now),
                Timestamp.from(now) // 填入成功：代表认领程度
            );
            return MessageClaimResult.claimed(claim); // 插入成功，代表认领成功
        } catch (DuplicateKeyException exception) { // 如果是重复插入：说明已经有人认领了
            duplicateClaimObserver.afterDuplicate(claim.consumerGroup(), claim.eventId()); // 查询当前数据库行信息
            ConsumptionRow existing = findRow(claim.consumerGroup(), claim.eventId()); // 查询现在已经有的记录
            if (existing == null) {  // 竞态场景：在主键冲突后到查询前行被删除了
                return MessageClaimResult.inProgress(); // 判定为处理中，等待下次重试
            }
            if (SUCCEEDED.equals(existing.status()) || DEAD_LETTERED.equals(existing.status())) {  // 如果状态是成功的或死信的
                return MessageClaimResult.duplicate(); // 判定为重复消息,告知上层无需在做
            }
            if (existing.leaseUntil() != null && existing.leaseUntil().isAfter(now)) { // 如果租约还没到
                return MessageClaimResult.inProgress(); // 判定为其他节点正在处理中

            }
            int changed = jdbc.update( // 。场景，前一个认领者的租约已超时，当前节点可以强制抢占处理权
                "UPDATE mq_consumption SET owner = ?, lease_until = ?, update_at = ? " +
                    "WHERE consumer_group = ? AND event_id = ? AND status = ?" +
                    "AND lease_until <= ?", // 乐观锁，仅当前租约确实已到期时更新
                claim.owner(),
                Timestamp.from(leaseUntil),
                Timestamp.from(now),
                claim.consumerGroup(),
                claim.eventId(),
                PROCESSING,
                Timestamp.from(now)
            ); // 填充参数
            return changed == 1  // 抢占是否成功
                ? MessageClaimResult.claimed(claim) // 成功抢占
                : MessageClaimResult.inProgress(); // 抢占失败（被别人抢走了）
        }
    }

    /**
     * 使用排他锁获取数据库的行信息
     */
    private ConsumptionRow lockRow(String consumerGroup, String eventId) {
        return jdbc.query( // 查询加锁
            "SELECT status,owner,lease_until FROM mq_consumption " + "WHERE consumer_group = ? AND event_id = ? FOR UPDATE", // FOR update 触发数据库行锁

            resultSet -> resultSet.next() // 处理结果
                ? new ConsumptionRow(resultSet.getString("status"), // 获取状态
                resultSet.getString("owner"), // 获取持有者
                toInstant(resultSet.getTimestamp("lease_until"))) // 获取租约到期时间
                : null, consumerGroup, eventId);
    }

    /**
     * 获取数据库的当前行信息
     */
    private ConsumptionRow findRow(String consumerGroup, String eventId) {
        return jdbc.query( // 普通查询
            "SELECT status,owner,lease_until FROM mq_consumption " +
                "WHERE consumer_group = ? AND event_id = ?",
            resultSet -> resultSet.next() // 处理结果类
                ? new ConsumptionRow(
                resultSet.getString("status"),
                resultSet.getString("owner"),
                toInstant(resultSet.getTimestamp("lease_until"))
            ) : null, consumerGroup, eventId);
    }

    /**
     * 函数式接口：用于重复认领回调监听
     */
    @FunctionalInterface
    interface DuplicateClaimObserver {
        void afterDuplicate(String consumerGroup, String eventId);
    }

    /**
     * 内部记录类：侧印数据库行数据
     */
    private record ConsumptionRow(String status, String owner, Instant leaseUntil) {
    }

    /**
     * 业务操作失败内部封装异常类，用于控制事务回滚
     */
    private static final class BusinessOperationFailure extends RuntimeException {
        private final Exception original; // 持有原始异常

        private BusinessOperationFailure(Exception original) {
            super(null, original, false, false); // 禁止堆栈填充以提供性能
            this.original = original;
        }

        private Exception original() {
            return original; // 获取原始异常类
        }
    }
}
