package com.yuegang.zhihui.notification.application;

import com.yuegang.zhihui.notification.security.NotificationSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Base64;

/**
 * Spring配置类，用于装配通知服务的 Bean
 */
@Configuration(proxyBeanMethods = false) // 声明为配置类，且不使用 CGLIB 代理以提升物理速度
public class NotificationConfiguration {

    @Bean
    NotificationService notificationService(DataSource d) { // 注册核心通知服务
        return new NotificationService(d); // 注入数据源并实例化
    }

    @Bean
    NotificationDispatchJob notificationDispatchJob(NotificationService s) { // 注册通知分发定时任务
        return new NotificationDispatchJob(s); // 注入服务类并实例化
    }

    @Bean
    NotificationQueryService notificationQueryService(DataSource d) { // 注入通知查询服务
        return new NotificationQueryService(d); // 注入数据源并实例化
    }

    @Bean
    NotificationSecurity notificationSecurity(@Value("${ygh.internal-request.hmac-base64}") String e) { // 注册安全组件
        byte[] k = Base64.getDecoder().decode(e); // 将 Base64 编码的密钥解码为字节数组
        try {
            return new NotificationSecurity(k); // 实例化安全校验器
        } finally {
            Arrays.fill(k, (byte) 0); // 实例化后立即擦除内存中的密钥，防止敏感信息泄露
        }
    }


}
