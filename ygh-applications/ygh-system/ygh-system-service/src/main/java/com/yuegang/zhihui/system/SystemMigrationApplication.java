package com.yuegang.zhihui.system;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

public final class SystemMigrationApplication { // 定义最终类：用于执行数据库增量迁移（Flyway）的独立程序，不可被继承
    private SystemMigrationApplication() { // 私有化构造函数，防止该类在外部被实例化（典型的工具类）
    } // 结束私有构造器

    public static void main(String[] a) { // 迁移程序的入口方法
        // 使用 try-with-resources 语法确保启动的Spring上下文任务执行完后能够自动关闭并释放数据库迁移
        try (var ignored = new SpringApplicationBuilder(SystemApplication.class) // 基于主配置类 SystemApplication 构建应用环境
            .web(WebApplicationType.NONE) // 核心设置：指向应用类型为非 web 环境，启动时不开启Tomcat或Netty服务器
            .run(a)) {//正式运行Spring容器，执行数据源初始化及Flyway迁移逻辑
//            代码运行到此处会自动触发close()方法关闭容器
        }
    } // 结束 main 方法
} // 结束类定义
