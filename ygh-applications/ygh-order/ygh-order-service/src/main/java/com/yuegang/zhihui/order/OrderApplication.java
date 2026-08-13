package com.yuegang.zhihui.order;

import com.yuegang.zhihui.common.mybatis.AuditorProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class OrderApplication {
    public static void main(String[] a) {
        SpringApplication.run(OrderApplication.class, a);
    }

    @Bean
    @ConditionalOnMissingBean
    AuditorProvider orderAuditorProvider() {
        return AuditorProvider.system();
    }
}
