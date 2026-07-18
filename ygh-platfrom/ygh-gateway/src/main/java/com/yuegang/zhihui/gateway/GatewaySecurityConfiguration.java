package com.yuegang.zhihui.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpMethod;
import java.util.LinkedHashSet;


/**响应式安全核心配置*/
/**网关全局API路由闭合式失败、响应式安全配置*/

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {
    // 配置公钥集的JWT解码器
    @Bean
    ReactiveJwtDecoder gatewayJwtDecoder(
            @Value("${ygh.security.jwt.issuer}") String issuer,
            @Value("${ygh.security.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${ygh.security.jwt.audience}") String audience
    ){
        var decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(GatewayJwtValidators.create(issuer,audience)); // 设置校验规则
        return decoder;

    }

    @Bean // 配置转换器
    Converter<Jwt, Mono<AbstractAuthenticationToken>> getwayJwtAuthenticationConverter(
            JwtPrincipalMapper principalMapper
    ){
        return jwt -> {
            var principal = principalMapper.map(jwt);
            var authorities = new LinkedHashSet<SimpleGrantedAuthority>();
            // 转换角色
            principal.roles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE" + role))
                    .forEach(authorities::add);
            //转换权限点
            principal.permissions().stream()
                    .map(permission -> new SimpleGrantedAuthority("PERM_" + permission))
                    .forEach(authorities::add);
            return Mono.just(new JwtAuthenticationToken(jwt, authorities, principal.userId()));
        };
    }

    // 核心安全过滤链
    @Bean
    SecurityWebFilterChain gatewaySecurityWebFilterChain(
            ServerHttpSecurity http,
            GatewaySecurityErrorWriter errorWriter,
            Converter<Jwt,Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter,
            OAuth2ResourceServerProperties oAuth2ResourceServerProperties){
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchange -> exchange
                        // 放行健康检查、文档和swagger
                        .pathMatchers("/actuator/health/**","/v3/api-docs/**", "/swagger-ui/**", "/livez","/readyz").permitAll()
                        // 放行认证相关的注册、登录】刷新令牌接口
                        .pathMatchers(HttpMethod.POST, "/api/vi/auth/register","/api/vi/auth/login","/api/v1/auth/refresh","/api/v1/auth/password-rest/**").permitAll()
                        // 放行图形验证码
                        .pathMatchers(HttpMethod.GET,"/api/v1/auth/captcha").permitAll()
                        // 放行只读性的公共业务接口（商品、类目、知识检索）
                        .pathMatchers(HttpMethod.GET,"/api/vi/products/**","/api/v1/product-catefories","/api/v1/product-brands","/api/v1/knowledg/dpcuments/**","/api/v1/knowledge/sreach").permitAll()
                        // 限制 仅限员工、管理员访问的业务
                        .pathMatchers("/api/v1/organization/**","/api/v1/training/**").hasAnyRole("EMPLOYEE","ADMIN")
                        // 限制 仅限超级管理员访问后台管理接口
                        .pathMatchers("/api/v1/auth/admin/**","/api/v1/system/**","/api/v1/roles/**","/api/v1/permissions/**").hasRole("ADMIN")
                        // 兜底 其余所有 /api/v1/** 请求必须通过认证
                        .pathMatchers("/api/v1/**").authenticated()
                        // 绝对防御 任何未在上面的请求
                        .anyExchange().denyAll())
                        // 异常处理，返回同意的401/403 json
                        .exceptionHandling(errors -> errors
                                .authenticationEntryPoint((exchange, ignored) -> errorWriter.unauthenticated(exchange))
                                .accessDeniedHandler((exchange, ignored) -> errorWriter.accessDenied(exchange)))
                                .oauth2ResourceServer(oauth2 -> oauth2
                                        .authenticationEntryPoint((exchange, ignored) -> errorWriter.unauthenticated(exchange))
                                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .build();

    }
}
