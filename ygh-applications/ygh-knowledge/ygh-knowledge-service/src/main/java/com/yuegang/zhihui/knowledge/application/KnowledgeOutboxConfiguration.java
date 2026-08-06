package com.yuegang.zhihui.knowledge.application;

import com.yuegang.zhihui.common.mq.DomainEventPublisher;
import com.yuegang.zhihui.common.mq.JdbcOutboxDispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "ygh.mq.enabled", havingValue = "true")
class KnowledgeOutboxConfiguration {
    @Bean(destroyMethod = "close")
    DomainEventPublisher knowledgeEventPublisher(@Value("${ygh.mq.nameserver}") String n, @Value("${ygh.mq.topic:YGH_DOMAIN_EVENTS}") String t) {
        return new RocketMqDomainEventPublisher("ygh-knowledge-producer", n, t);
    }

    @Bean
    KnowledgeOutboxJob knowledgeOutboxJob(JdbcTemplate j, DomainEventPublisher p) {
        return new KnowledgeOutboxJob(new JdbcOutboxDispatcher(j, p, "knowledge_outbox"));
    }

    static final class KnowledgeOutboxJob {
        private final JdbcOutboxDispatcher d;

        KnowledgeOutboxJob(JdbcOutboxDispatcher x) {
            d = x;
        }

        @Scheduled(fixedDelayString = "${ygh.mq.dispatch-delay:1000}")
        public void dispatch() {
            d.dispatch();
        }
    }
}
