package com.yuegang.zhihui.user.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;

/**
 * Spring配置类，用于将安全相关的类注入到 IoC 容器中
 */
@Configuration(proxyBeanMethods = false) // 声明为配置类，不代理 Bean 方法以提升性能
public class UserSecurityConfiguration { // 定义安全配置类

    @Bean
        // 将方法返回值注册为 Spring Bean
    TrustedUserContextResolver trustedUserContextResolver(
        @Value("${ygh.internal-request.hmac-base64}") String encodedSecret, Clock clock) { // 注入 Base64 编码的密钥和时钟
        byte[] secret; // 声明字节数组用于存储密钥
        try {
            secret = Base64.getDecoder().decode(encodedSecret);
        } // 解码 Base64 格式的密码
        catch (IllegalArgumentException malformed) {
            throw new IllegalStateException("internal request secret is malformed", malformed);
        } // 解码失败抛出异常
        try {
            return new TrustedUserContextResolver(secret, clock);
        } // 实例化解器
        finally {
            Arrays.fill(secret, (byte) 0);
        } // 安全擦除内存中的敏感密钥数据
    }

    @Bean
        // 将内部服务验证器注册为 Spring Bean
    UserInternalServiceVerifier userInternalServiceVerifier(
        @Value("${ygh.internal-request.hmac-base64}") String encodedSecret) { // 注入相同内部的请求密钥
        byte[] secret = Base64.getDecoder().decode(encodedSecret); // 解码密钥
        try {
            return new UserInternalServiceVerifier(secret);
        } // 实例化验证器
        finally {
            Arrays.fill(secret, (byte) 0);
        } // 安全擦车内存中的敏感密钥数据
    }
}
