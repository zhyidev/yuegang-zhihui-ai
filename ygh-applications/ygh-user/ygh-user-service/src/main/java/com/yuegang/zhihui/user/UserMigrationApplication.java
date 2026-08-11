package com.yuegang.zhihui.user;
/*
 * 该类是一个专门用于数据库迁移（Flyway）的任务启动类
 */

/*
 * Bounded Flyway job; production runtime starts with Flyway disabled.
 */

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * 限定范围的 Flyway 任务：生产环境运行时默认会禁用 Flyway。
 */
public final class UserMigrationApplication { // 定义数据库迁移应用类，使用 final 关键字防止被继承

    private UserMigrationApplication() {
    } // 私有构造函数，防止该类被实例化，因为它仅提供静态入口

    static void main(String[] args) {
        try (var ignored = new SpringApplicationBuilder(UserApplication.class) // 使用主应用的配置作为基础来构建
                .web(WebApplicationType.NONE) // 设置应用类型为 NONE，即不启动嵌入式 Web 服务器（Tomcat 等）
                .properties("spring.cloud.nacos.discovery.enabled=false") // 设置临时属性：禁
                .run(args)) { // 运行应用，并利用 try-with-resources 确保运行结束后自动关闭上下文

        }
    }
}
