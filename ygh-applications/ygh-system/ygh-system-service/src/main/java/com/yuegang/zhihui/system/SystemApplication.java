package com.yuegang.zhihui.system;

import com.yuegang.zhihui.common.mybatis.AuditorProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication //标注这是一个Spring Boot核心应用程序，启用自动配置,组件扫描和配置类功能
public class SystemApplication { // 定义系统服务主启动类
    public static void main(String[] args) { // 程序执行的入口方法，接收命令行参数
        SpringApplication.run(SystemApplication.class, args); // 调用 Spring 框架的 run 方法
    }

    @Bean // 将此方法的返回值作为Bean 注册到Spring容器中，该其他模块(如MyBatis 审计)使用
    @ConditionalOnMissingBean // 条件装配：仅当 Spring 容器中没有其他 AuthorProvider 类型的 Bean 时，才执行默认配置
    AuditorProvider systemAuditorProvider() { //定义提供系统审计人的方法
    return AuditorProvider.system();  // 返回公共模块预设的系统级审计人提供者（通常默认表示为 “SYSTEM”）
    } // 结束 Bean 定义
} // 结束类定义