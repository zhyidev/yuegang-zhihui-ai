package com.yuegang.zhihui.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 粤港甄选网关服务启动类。
 *
 * <p>基于 Spring Cloud Gateway 的 API 网关入口，
 * 负责统一路由转发、安全校验、限流等横切关注点。</p>
 *
 * @author yuegang-zhihui
 * @since 1.0.0
 */
@SpringBootApplication
public class GatewayApplication {

    /**
     * 网关服务主入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
