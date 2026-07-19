package com.yuegang.zhihui.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 网关边界安全配置。
 *
 * <p>负责创建请求守卫过滤器，对请求体大小和上传路径做安全限制。</p>
 *
 * @author yuegang-zhihui
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class GatewayEdgeConfiguration {

    /**
     * 创建请求守卫过滤器 Bean。
     *
     * @param requestMaxSize 普通请求最大 Body 大小
     * @param uploadMaxSize  上传请求最大 Body 大小
     * @param uploadPaths    允许上传的路径列表（逗号分隔）
     * @param errorWriter    安全错误响应写入器
     * @return 配置好的请求守卫过滤器
     */
    @Bean
    GatewayRequestGuardFilter gatewayRequestGuardFilter(
            @Value("${ygh.gateway.request.max-size:2MB}") DataSize requestMaxSize,
            @Value("${ygh.gateway.request.upload-max-size:50MB}") DataSize uploadMaxSize,
            @Value("${ygh.gateway.request.upload-paths}") String uploadPaths,
            GatewaySecurityErrorWriter errorWriter) {
        return new GatewayRequestGuardFilter(
                requestMaxSize.toBytes(),
                uploadMaxSize.toBytes(),
                parsePaths(uploadPaths),
                errorWriter);
    }

    /**
     * 将逗号分隔的上传路径字符串解析为不可变集合。
     *
     * @param configuredPaths 配置文件中的路径列表（逗号分隔）
     * @return 上传路径集合（不可变）
     */
    static Set<String> parsePaths(String configuredPaths) {
        var paths = new LinkedHashSet<String>();
        Arrays.stream(configuredPaths.split(","))
                .map(String::trim)
                .filter(path -> !path.isEmpty())
                .forEach(paths::add);
        return Set.copyOf(paths);
    }
}
