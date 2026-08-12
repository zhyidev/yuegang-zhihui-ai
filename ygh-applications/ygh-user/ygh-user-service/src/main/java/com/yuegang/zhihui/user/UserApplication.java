package com.yuegang.zhihui.user;

import com.yuegang.zhihui.common.mybatis.AuditorProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication // 标识为这是一个 Spring Boot 应用，开启自动配置，组件扫描和配置类功能
public class UserApplication { // 定义用户服务启动类
    public static void main(String[] args) { // 程序入口启动方法
        SpringApplication.run(UserApplication.class, args); // 启动 Spring 应用上下文并运行应用
    }

    @Bean // 将方法返回的对象注册到 Spring 容器中 no usages
    @ConditionalOnMissingBean
    AuditorProvider userAuditorProvider() {
        return AuditorProvider.system();
    }
}
