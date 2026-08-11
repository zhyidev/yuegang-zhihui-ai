package com.yuegang.zhihui.gateway;

import org.springframework.core.Ordered;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;

/**
 * 网关 CORS 过滤器 —— 扩展 Spring 原生的 {@link CorsWebFilter} 并控制执行顺序。
 *
 * <p>将 CORS 处理提前到 {@link Ordered#HIGHEST_PRECEDENCE} + 12，
 * 确保在安全过滤器之前完成跨域预检（OPTIONS），避免认证逻辑拦截 CORS 预检请求。</p>
 *
 * @author yuegang-zhihui
 * @since 1.0.0
 */
final class GatewayCorsWebFilter extends CorsWebFilter implements Ordered {

    /**
     * 构造器，接收 CORS 配置源。
     *
     * @param configurationSource CORS 规则配置源
     */
    GatewayCorsWebFilter(CorsConfigurationSource configurationSource) {
        super(configurationSource);
    }

    /**
     * CORS 过滤器执行顺序。
     *
     * <p>HIGHEST_PRECEDENCE + 12，在 CorrelationIdFilter 之后、
     * 安全过滤器之前执行。</p>
     *
     * @return 执行优先级
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 12;
    }
}

