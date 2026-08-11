package com.yuegang.zhihui.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuegang.zhihui.notification.api.NotificationCommand;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * RocketMQ消费者，监听领域事件并转化为通知
 */
public final class NotificationDomainEventConsumer implements AutoCloseable { // 实现自动关闭接口以释放 MQ 连接
    private final DefaultMQPushConsumer consumer; // 声明 RocketMQ 推模式消费者

    public NotificationDomainEventConsumer(String nameserver, String topic, NotificationService service, ObjectMapper json) {
        try {
            consumer = new DefaultMQPushConsumer("ygh-notification-domain-events"); // 初始化消费者组
            consumer.setNamesrvAddr(nameserver); // 设置 MQ 的名字服务器地址
            // 订阅特定主题下的多类业务标签，分配培训、钱包支付成功、知识库审核通过
            consumer.subscribe(topic, "TRAINING_ASSIGNED || WALLET_PAYMENT_SUCCEESSED || KNOWLEDGE_REVIEWED");
            consumer.setMaxReconsumeTimes(8); // 设置最大重试次数为 8 次
            consumer.registerMessageListener((MessageListenerConcurrently) (List<MessageExt> messages, ConsumeConcurrentlyContext context) -> { // 注册并发监听器
                for (var message : messages) { // 遍历接收到的消息列表
                    try {
                        // 将消息解析为 Map 结构
                        @SuppressWarnings("unchecked")
                        Map<String, Object> p = json.readValue(new String(message.getBody(), StandardCharsets.UTF_8), Map.class);
                        // 提取标签（事件类型）和唯一 ID（防重）
                        String tag = message.getTags(), event = Objects.requireNonNullElse(message.getKeys(), UUID.randomUUID().toString());

                        if ("TRAINING_ASSIGNED".equals(tag)) // 如果是培训分配事件
                            service.create(new NotificationCommand(event, text(p, "userId"), "TRAINING_ASSIGNED", Map.of("courseTitle", text(p, "courseTitle"))));
                        else if ("WALLET_PAYMENT_SUCCEEDED".equals(tag)) // 如果是支付成功
                            service.create(new NotificationCommand(event, text(p, "userId"), "ORDER_PAID", Map.of("referenceId", text(p, "referenceId"))));
                        else if ("KNOWLEDGE_REVIEWED".equals(tag)) // 如果是知识审核事件
                            service.create(new NotificationCommand(event, text(p, "userId"), "KNOWLEDGE_REVIEWED", Map.of("title", text(p, "title"), "decision", text(p, "decision"))));

                    } catch (Exception e) { // 消费处理失败
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER; // 告知 MQ 稍后重新投递进行重试
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS; // 告知 MQ 消费成功
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String text(Map<String, Object> p, String k) { // 辅助方法：从解析的 JSON Map 中提取必填字符串
        Object v = p.get(k);
        if (v == null || v.toString().isBlank())
            throw new IllegalArgumentException("event field missing"); // 字段缺失则抛出参数异常
        return v.toString();
    }


    @Override
    public void close() throws Exception {
        consumer.shutdown(); // 关闭消费者，释放资源
    }
}
