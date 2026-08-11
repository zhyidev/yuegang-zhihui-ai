package com.yuegang.zhihui.inventory;

import com.yuegang.zhihui.common.mybatis.AuditorProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class InventoryApplication {
    public static void main(String[] a) {
        SpringApplication.run(InventoryApplication.class, a);
    }

    @Bean
    @ConditionalOnMissingBean
    AuditorProvider inventoryAuditorProvider() {
        return AuditorProvider.system();
    }
}
