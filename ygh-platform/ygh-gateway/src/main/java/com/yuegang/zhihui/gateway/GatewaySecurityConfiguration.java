package com.yuegang.zhihui.gateway;




import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
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

import java.util.LinkedHashSet;

/**
 * 网关响应式安全核心配置。
 *
 * <p>采用 Spring Security WebFlux + OAuth2 Resource Server 模式，
 * 实现基于 JWT 的认证与授权。所有未通过验证的请求将被拒绝（闭合式失败）。</p>
 *
 * <p>配置要点：</p>
 * <ul>
 *   <li>从 JWK Set URI 动态获取公钥，使用 RS256 算法验证 JWT 签名；</li>
 *   <li>自定义验证器链：校验 iss（发行者）、exp（过期时间）、aud（受众）；</li>
 *   <li>将 JWT Claims 转换为 Spring Security 权限体系（角色 + 权限点）。</li>
 * </ul>
 *
 * @author yuegang-zhihui
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

    /**
     * 配置基于 JWK Set 的响应式 JWT 解码器。
     *
     * <p>解码器从配置的 JWK Set URI 动态拉取公钥，
     * 并使用 RS256 算法验证 JWT 签名，同时附加自定义验证器链。</p>
     *
     * @param issuer    令牌发行者
     * @param jwkSetUri JWK Set 端点 URI
     * @param audience  令牌受众
     * @return 响应式 JWT 解码器实例
     */
    @Bean
    ReactiveJwtDecoder gatewayJwtDecoder(
            @Value("${ygh.security.jwt.issuer}") String issuer,
            @Value("${ygh.security.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${ygh.security.jwt.audience}") String audience) {
        // 使用 Nimbus 库从 JWK Set URI 构建解码器
        var decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)   // RSA 256 签名算法
                .build();
        // 附加自定义验证器（issuer + audience）
        decoder.setJwtValidator(GatewayJwtValidators.create(issuer, audience));
        return decoder;
    }

    /**
     * 配置 JWT → Spring Security 权限对象的转换器。
     *
     * <p>转换逻辑：</p>
     * <ol>
     *   <li>调用 {@link JwtPrincipalMapper} 将 JWT Claims 映射为系统内部主体；</li>
     *   <li>角色加前缀 {@code ROLE_}，权限点加前缀 {@code PERM_}；</li>
     *   <li>封装为 {@link JwtAuthenticationToken} 供 Spring Security 授权决策。</li>
     * </ol>
     *
     * @param principalMapper JWT 主体映射器
     * @return 转换器实例
     */
    @Bean
    Converter<Jwt, Mono<AbstractAuthenticationToken>> gatewayJwtAuthenticationConverter(
            JwtPrincipalMapper principalMapper) {
        return jwt -> {
            // 将 JWT Claims 映射为系统内部主体
            var principal = principalMapper.map(jwt);

            var authorities = new LinkedHashSet<SimpleGrantedAuthority>();

            // 角色 → ROLE_xxx
            principal.roles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .forEach(authorities::add);

            // 权限点 → PERM_xxx
            principal.permissions().stream()
                    .map(perm -> new SimpleGrantedAuthority("PERM_" + perm))
                    .forEach(authorities::add);

            return Mono.just(new JwtAuthenticationToken(jwt, authorities, principal.userId()));
        };
    }

    // 核心安全过滤链，配置谁能访问哪些接口
    @Bean
    SecurityWebFilterChain gatewaySecurityWebFilterChain(
            ServerHttpSecurity http,
            GatewaySecurityErrorWriter errorWriter,
            Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter
    ) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable) // 禁用 CSRF
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable) // 禁用 Basic 认证
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable) // 禁用表单登录
                .logout(ServerHttpSecurity.LogoutSpec::disable) // 禁用登出
                // 设置为无状态服务，不存储对话
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(exchange -> exchange
                        // 1. 放行健康检查、文档和 Swagger 路径
                        .pathMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger/**", "/livez", "/readyz").permitAll()
                        // 2. 放行认证相关的注册、登录、刷新令牌接口
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "api/v1/auth/refresh", "api/v1/auth/password-rest/**").permitAll()
                        // 3. 放行图形验证码
                        .pathMatchers(HttpMethod.GET, "/api/v1/auth/captcha").permitAll()
                        // 4. 放行只读性的公共业务接口（商品、类目、知识检索等）
                        .pathMatchers(HttpMethod.GET, "/api/v1/product/**", "/api/v1/product-categories", "/api/v1/knowledge/dpcumets/**", "/apt/v1/knowledge/search").permitAll()
                        // 5. 限制：仅限员工和管理员访问的业务
                        .pathMatchers("/api/v1/organization/**", "/api/v1/training/**").hasAnyRole("EMPLOYEE", "ADMIN")
                        // 6. 限制：仅限超级管理员访问的后台管理接口
                        .pathMatchers("/api/v1/auth/admin/**", "/api/v1/admin/**", "/api/v1/system/**", "/api/v1/roles/**", "/api/v1/permissions/**").hasAnyRole("ADMIN")
                        // 7. 兜底：其余所有 /api/v1/** 请求必须经过认证
                        .pathMatchers("/api/v1/**").authenticated()
                        .anyExchange().denyAll())
                        // 异常处理逻辑（返回统一的 401/403 JSON）
                        .exceptionHandling(errors -> errors
                                .authenticationEntryPoint((exchange, ignored) -> errorWriter.unauthenticated(exchange))
                                .accessDeniedHandler((exchange, ignored) -> errorWriter.accessDenied(exchange)))
                                .oauth2ResourceServer(resourceServer -> resourceServer
                                        .authenticationEntryPoint((exchange, ignored) -> errorWriter.unauthenticated(exchange))
                                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                                .build();

    }


}
