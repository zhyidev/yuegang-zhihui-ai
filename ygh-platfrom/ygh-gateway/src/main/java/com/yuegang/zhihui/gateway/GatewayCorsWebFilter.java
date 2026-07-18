package com.yuegang.zhihui.gateway;

import org.springframework.core.Ordered;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;

// 自定义 CORS 过滤器，确保在网关中按高优先级执行
final class GatewayCorsWebFilter extends CorsWebFilter implements Ordered {
    GatewayCorsWebFilter(CorsConfigurationSource configurationSource){
        super(configurationSource);
    }

    // 保证 CORS 过滤器在请求处理链的高优先级位置执行
    @Override
    public int getOrder(){
        return Ordered.HIGHEST_PRECEDENCE + 12;
    }
}
