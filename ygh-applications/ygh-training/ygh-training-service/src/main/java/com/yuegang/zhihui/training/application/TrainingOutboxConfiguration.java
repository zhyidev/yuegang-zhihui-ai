package com.yuegang.zhihui.training.application;

import com.yuegang.zhihui.common.mq.DomainEventPublisher;
import com.yuegang.zhihui.common.mq.JdbcOutboxDispatcher;
import com.yuegang.zhihui.common.mq.RocketMqDomainEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "ygh.mq.enabled", havingValue = "true")
class TrainingOutboxConfiguration {
    @Bean(destroyMethod = "close")
    DomainEventPublisher trainingEventPublisher(@Value("${ygh.mq.nameserver}") String n, @Value("${ygh.mq.topic:YGH_DOMAIN_EVENTS}") String t) {
        return new RocketMqDomainEventPublisher("ygh-training-producer", n, t);
    }

    @Bean
    TrainingOutboxJob trainingOutboxJob(JdbcTemplate j, DomainEventPublisher p) {
        return new TrainingOutboxJob(new JdbcOutboxDispatcher(j, p, "training_outbox"));
    }

    static final class TrainingOutboxJob {
        private final JdbcOutboxDispatcher dispatcher;

        TrainingOutboxJob(JdbcOutboxDispatcher d) {
            dispatcher = d;
        }

        @Scheduled(fixedDelayString = "${ygh.mq.dispatch-delay:1000}")
        public void dispatch() {
            dispatcher.dispatch();
        }
    }
}
