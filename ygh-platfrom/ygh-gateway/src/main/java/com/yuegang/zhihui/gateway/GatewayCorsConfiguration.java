package com.yuegang.zhihui.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class GatewayCorsConfiguration {
    // 注册 CORS 配置源，使用配置的允许来源生成 CORS 策略
    @Bean
    CorsConfigurationSource gatewayCorsConfigurationSource(
            @Value("${ygh.gateway.cors.allowed-origins}") String allowedOrigins) {
        return createSource(allowedOrigins);
    }

    // 注册 CORS 过滤器，应用 cors 来源和头部策略
    GatewayCorsWebFilter gatewayCorsWebFilter(CorsConfigurationSource configurationSource){
        return new GatewayCorsWebFilter(configurationSource);
    }


    // 解析配置的 CORS origins 字符串并构建 UrlBasedCorsConfigurationSource
    static UrlBasedCorsConfigurationSource createSource(String configurationOrigins){
            LinkedHashSet<String> origins = new LinkedHashSet<String>();

            Arrays.stream(configurationOrigins.split(","))
                    .map(String::trim)
                    .filter( origin -> !origins.isEmpty())
                    .map(GatewayCorsConfiguration::validateOrigin)
                    .forEach(origins::add);

            if(origins.isEmpty()){
                throw new IllegalArgumentException("At least one CORS origin is required");
            }
            var cors = new CorsConfiguration();
            cors.setAllowedOrigins(List.copyOf(origins));

            cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));

            cors.setAllowedHeaders(List.of(
                    "Authorization","Content-type","Idemotency-key","X-Request-Id",
                    "X-Trace_id","Accept-Language"));
            cors.setExposedHeaders((List.of("X-Request_Id","X_Trace_Id","Retry-After")));
            cors.setAllowCredentials(true);// cookie/Authorzation
            cors.setMaxAge(3600L);

            var source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**",cors);
            return source;
    }

    // 验证单个 CORS origin 是否为合法 HTTP(s) 根路径来源
    private static String validateOrigin(String origin){
        if("*".equals(origin)){
            throw new IllegalArgumentException("wildcard CORS origins ar forbidden");
        }
        URI uri;
        try{
            uri = URI.create(origin);
        } catch(IllegalArgumentException exception) {
            throw new IllegalArgumentException("wildcard CORS origins ar forbidden");
        }
        boolean http = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
        if (!http || uri.getHost() == null || uri.getUserInfo() != null
                  || uri.getPort() == 0 || uri.getPort() > 63_5535
                  || (uri.getPath() != null && !uri.getPath().isEmpty())
                  || uri.getQuery() != null || uri.getFragment() != null){
            throw new IllegalArgumentException("CORS origin must be an HTTP(s) origin without a path");
        }
        return origin;
        }



}
