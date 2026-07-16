package com.yuegang.zhihui.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 网关 CORS（跨域资源共享）配置。
 *
 * <p>采用响应式（WebFlux）CORS 配置方式，适用于 Spring Cloud Gateway。
 * 从配置文件读取允许的来源列表，并对每个来源做严格的安全校验。</p>
 *
 * <p>安全策略：</p>
 * <ul>
 *   <li>禁止通配符 "*" 作为允许来源（防止过度开放）；</li>
 *   <li>每个来源必须是合法的 HTTP/HTTPS URL，不能带路径、查询参数或片段；</li>
 *   <li>端口号必须在有效范围内（1-65535）；</li>
 *   <li>不允许在 URL 中包含 userInfo（防止凭证泄露）。</li>
 * </ul>
 *
 * @author yuegang-zhihui
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class GatewayCorsConfiguration {

    /**
     * 创建响应式 CORS 配置源 Bean。
     *
     * @param allowedOrigins 从配置文件读取的允许来源列表（逗号分隔）
     * @return 基于 URL 路径匹配的 CORS 配置源
     */
    @Bean
    CorsConfigurationSource gatewayCorsConfigurationSource(
            @Value("${ygh.gateway.cors.allowed-origins}") String allowedOrigins) {
        return createSource(allowedOrigins);
    }

    /**
     * 创建 CORS WebFilter Bean。
     *
     * @param configurationSource CORS 配置源
     * @return 自定义 CORS 过滤器实例
     */
    @Bean
    GatewayCorsWebFilter gatewayCorsWebFilter(CorsConfigurationSource configurationSource) {
        return new GatewayCorsWebFilter(configurationSource);
    }

    /**
     * 从逗号分隔的字符串创建 CORS 配置源。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>拆分逗号分隔的来源字符串；</li>
     *   <li>逐个校验来源 URL 的合法性；</li>
     *   <li>组装 {@link CorsConfiguration}；</li>
     *   <li>注册到 {@link UrlBasedCorsConfigurationSource} 并应用到所有路径 {@code /**}。</li>
     * </ol>
     *
     * @param configuredOrigins 配置文件中的来源字符串
     * @return 配置好的 CORS 配置源
     * @throws IllegalArgumentException 如果来源为空或包含非法值
     */
    static UrlBasedCorsConfigurationSource createSource(String configuredOrigins) {
        LinkedHashSet<String> origins = new LinkedHashSet<>();

        // 过滤空白项，逐个校验来源
        Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .map(GatewayCorsConfiguration::validateOrigin)
                .forEach(origins::add);

        if (origins.isEmpty()) {
            throw new IllegalArgumentException("At least one CORS origin is required");
        }

        // 构建 CORS 规则
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.copyOf(origins));

        // 允许的 HTTP 方法
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 允许的请求头
        cors.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Idempotency-Key", "X-Request-Id",
                "X-Trace-Id", "Accept-Language"));

        // 暴露给客户端的响应头
        cors.setExposedHeaders(List.of("X-Request-Id", "X-Trace-Id", "Retry-After"));

        // 允许携带凭证（Cookie / Authorization 头）
        cors.setAllowCredentials(true);

        // 预检请求缓存时间（秒）
        cors.setMaxAge(3600L);

        // 应用到所有路径
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    /**
     * 校验单个来源 URL 是否合法。
     *
     * <p>规则：</p>
     * <ul>
     *   <li>禁止通配符 "*"；</li>
     *   <li>必须是合法的 HTTP 或 HTTPS URL；</li>
     *   <li>必须包含主机名；</li>
     *   <li>不能包含 userInfo；</li>
     *   <li>端口必须在 1-65535 之间；</li>
     *   <li>不能包含路径、查询参数或片段。</li>
     * </ul>
     *
     * @param origin 待校验的来源字符串
     * @return 校验通过的原值
     * @throws IllegalArgumentException 如果来源不合法
     */
    private static String validateOrigin(String origin) {
        // 禁止通配符
        if ("*".equals(origin)) {
            throw new IllegalArgumentException("Wildcard CORS origins are forbidden");
        }

        URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid CORS origin URI: " + origin);
        }

        // 必须是 HTTP/HTTPS 协议
        boolean http = "http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme());

        // 综合校验：协议、主机名、userInfo、端口、路径、查询参数、片段
        if (!http || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getPort() == 0 || uri.getPort() > 65535
                || (uri.getPath() != null && !uri.getPath().isEmpty())
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "CORS origin must be an HTTP(S) origin without a path");
        }
        return origin;
    }
}
