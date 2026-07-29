package com.yuegang.zhihui.system.application;

import com.yuegang.zhihui.system.domain.AuthorizationRepository;
import com.yuegang.zhihui.system.infrastructure.JdbcAuthorizationRepository;
import com.yuegang.zhihui.system.security.InternalServiceVerifier;
import com.yuegang.zhihui.system.security.SystemSecretCipher;
import com.yuegang.zhihui.system.security.SystemTrustedUserContextResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;

// 该类作为 Spring Boot 配置中心，手动编排并注入所有业务 Bean 与安全组件。
@Configuration(proxyBeanMethods = false) // 标识为配置类，且关闭运行时代理以提升性能
public class SystemConfiguration {
    @Bean // 注入 Bean,授权仓储实现
    AuthorizationRepository authorizationRepository(DataSource dataSource) {
        return new JdbcAuthorizationRepository(dataSource); // 使用 JDBC 实现的授权仓储
    }

    @Bean // 注册 Bean ，应用层授权服务
    AuthorizationService authorizationService(AuthorizationRepository repository) {
        return new AuthorizationService(repository);
    }

    @Bean // 注册 Bean，角色管理服务
    RoleAdministrationService roleAdministrationService(DataSource dataSource) {
        return new RoleAdministrationService(dataSource);
    }

    // Bean
    //SystemSettingservice

    @Bean // 注册 Bean：系统密文加密器
    SystemSecretCipher systemSecretCipher(@Value("${ygh.system.config-master-key-base64}") String encoded) {
        byte[] key  = Base64.getDecoder().decode(encoded); // 解码 Base64 格式的主密钥
        try{
            return new SystemSecretCipher(key); // 初始化加密器
        }catch(Exception e){
           Arrays.fill(key, (byte)0); // 关键安全操作，立刻擦除内存终端明文主密钥
        }
    }

    @Bean // 注册 Bean：AI 供应商配置服务
    AiProviderConfigService aiProviderConfigService(DataSource dataSource,SystemSecretCipher secrets) {
        return new AiProviderConfigService(dataSource,secrets);

    }


    @Bean // 注册 Bean：系统通用查询服务
    SystemCatalogService systemCatalogService(DataSource dataSource) {
        return new SystemCatalogService(dataSource);// 注入数据源
    }

//    @Bean
//    SystemDictionaryAdministrationService

    @Bean  // 注册Bean：受信任的用户上下文解析器
    SystemTrustedUserContextResolver systemTrustedUserContextResolver(@Value("${ygh.internal-request.hmac-base64}") String encoded, Clock clock) {
        byte[] secret  = Base64.getDecoder().decode(encoded); // 解码 Base64 格式的密钥
        try {
            return new SystemTrustedUserContextResolver(secret,clock); // 初始化解析器
        }finally {
            Arrays.fill(secret, (byte)0); // 擦除内存中的密钥字节
        }
    }

    @Bean // 注册Bean，内存服务问题调出校验器
    InternalServiceVerifier internalServiceVerifier(@Value("${ygh.internal-request.hmac-base64}") String encoded, Clock clock) {
        byte[] secret  = Base64.getDecoder().decode(encoded); // 解码 Base64 格式的密钥
        try {
            return new InternalServiceVerifier(secret,clock); // 初始化验证器
        }finally {
            Arrays.fill(secret, (byte)0); // 擦除内存中的密钥字节
        }
    }

    @Bean // 注册 Bean: 系统全局时钟
    Clock systemClock() {
        return Clock.systemUTC(); // 统一使用 UTC 标准时钟
    }

}