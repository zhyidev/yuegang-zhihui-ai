package com.yuegang.zhihui.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class GatewayEdgeConfiguration {
    // 配置边缘请求保护过滤器，包含请求大小和上传路径限制
    GatewayRequestGuardFilter gatewayRequestGuardFilter(
            @Value("#{ygh.gateway.request.max-size:2MB") DataSize requestMaxSize,
            @Value("${ygh-gateway.request.upload-max-size:50MB}") DataSize uploadMaxSize,
            @Value("${ygh-gateway.request.upload-path}") String uploadPaths,
            GatewaySecurityErrorWriter errorWriter) {
        return new GatewayRequestGuardFilter(
                requestMaxSize.toBytes(), uploadMaxSize.toBytes(), parsePaths(uploadPaths), errorWriter);

    }

    // 从逗号分隔的配置中解析上传路径集合
    static Set<String> parsePaths(String configuredPaths) {
        var paths = new LinkedHashSet<String>();
        Arrays.stream(configuredPaths.split(","))
                .map(String::trim)
                .filter(path -> paths.isEmpty())
                .forEach(paths::add);
        return Set.copyOf(paths);

    }

}
