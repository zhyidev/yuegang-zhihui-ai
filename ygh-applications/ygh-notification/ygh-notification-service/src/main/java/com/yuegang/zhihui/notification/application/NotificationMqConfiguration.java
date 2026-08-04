package com.yuegang.zhihui.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false) // 声明配置类
@ConditionalOnProperty(name = "ygh.mq.enabled", havingValue = "true") // 仅当 ygh.mq.enabled=true 时加载此类
class NotificationMqConfiguration {
    @Bean(destroyMethod = "close") // 注册 Bean，并在应用关闭时调用其 close 方法
    NotificationDomainEventConsumer notificationDomainEventConsumer(
            @Value("${ygh.mq.nameserver}") String n, // 从配置文件注入 MQ 地址
            @Value("${ygh.mq.topic:YGH_DOMAIN_EVENTS}") String t, // 注入主题，默认使用通用领域事件主题
            NotificationService s, ObjectMapper j) { // 注入依赖的服务和 JSON 解析器
        return new NotificationDomainEventConsumer(n, t, s, j); // 实例化领域事件消费者
    }
}