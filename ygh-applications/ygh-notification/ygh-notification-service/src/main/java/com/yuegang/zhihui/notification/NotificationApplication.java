package com.yuegang.zhihui.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling // 开启 Spring 的定时任务调度功能，使应用能够识别并执行 @Scheduled 标注的方法
@SpringBootApplication // 标注这是一个 Spring Boot 应用程序的核心启动类，包含了配置、自动装配和组件扫描功能
public class NotificationApplication { // 定义添加服务的启动类

    public static void main(String[] a) { // 程序执行的入口
        SpringApplication.run(NotificationApplication.class, a); // 启动方法，用于初始化 Spring 容器并运行整个应用服务
    }

}
