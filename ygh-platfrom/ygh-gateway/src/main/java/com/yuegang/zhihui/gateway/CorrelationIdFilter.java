package com.yuegang.zhihui.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    // 定义安全的 ID 正则表达式，只允许特定字母、数字和分隔符
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{7,63}");

    // 内部 ID 生成构造者，用于在请求头中缺少合法 id 时生成新 id
    private final Supplier<String> idGenerator;

    // 使用 UUID 生成默认安全 ID
    CorrelationIdFilter() {
        this(() -> UUID.randomUUID().toString().replace("-", ""));
    }

    CorrelationIdFilter(Supplier<String> idGenerator) {
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
    }

    // 将 traceId 与 requestId 注入请求与响应，保证链路追踪信息可用
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = resolveId(exchange.getRequest().getHeaders().getFirst(GatewayHeaders.TRACE_ID));
        String requestId = resolveId(exchange.getRequest().getHeaders().getFirst(GatewayHeaders.REQUEST_ID));

        var request = exchange.getRequest().mutate()
                .header(GatewayHeaders.TRACE_ID, traceId)
                .header(GatewayHeaders.REQUEST_ID, requestId)
                .build();

        var correlated = exchange.mutate().request(request).build();

        correlated.getAttributes().put(GatewaySecurityAttributes.TRACE_ID, traceId);
        correlated.getAttributes().put(GatewaySecurityAttributes.REQUEST_ID, requestId);

        correlated.getResponse().getHeaders().set(GatewayHeaders.TRACE_ID, traceId);
        correlated.getResponse().getHeaders().set(GatewayHeaders.REQUEST_ID, requestId);

        correlated.getResponse().beforeCommit(() -> {
            correlated.getResponse().getHeaders().set(GatewayHeaders.TRACE_ID, traceId);
            correlated.getResponse().getHeaders().set(GatewayHeaders.REQUEST_ID, requestId);
            return Mono.empty();
        });

        return chain.filter(correlated);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    // 校验请求头中的 id 是否安全，不安全时使用生成器生成新 id
    private String resolveId(String candidate) {
        if (candidate != null && SAFE_ID.matcher(candidate).matches()) {
            return candidate;
        }
        String generated = idGenerator.get();
        if (generated == null || !SAFE_ID.matcher(generated).matches()) {
            throw new IllegalStateException("Correlation ID generator returned an unsafe value");
        }
        return generated;
    }
}
