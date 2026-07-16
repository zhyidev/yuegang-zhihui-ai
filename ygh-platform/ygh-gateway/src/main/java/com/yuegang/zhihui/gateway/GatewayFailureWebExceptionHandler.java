package com.yuegang.zhihui.gateway;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * 网关全局异常处理器。
 *
 * <p>在过滤器链发生异常时统一拦截，将技术异常转换为标准业务错误响应。
 * 当前支持两类异常：</p>
 * <ul>
 *   <li>{@link BlockException}（Sentinel 限流/熔断）→ 429 限流响应；</li>
 *   <li>{@link NotFoundException}（路由未找到）→ 503 服务不可用响应；</li>
 *   <li>其他异常 → 向上抛出，由 Spring 默认处理器接管。</li>
 * </ul>
 *
 * <p>优先级设为 -2，确保在 Spring 默认异常处理器之前执行。</p>
 *
 * @author yuegang-zhihui
 * @since 1.0.0
 */
public class GatewayFailureWebExceptionHandler implements WebExceptionHandler, Ordered {

    /** 安全错误响应写入器 */
    private final GatewaySecurityErrorWriter errorWriter;

    /**
     * 构造器。
     *
     * @param errorWriter 安全错误响应写入器实例
     */
    GatewayFailureWebExceptionHandler(GatewaySecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    /**
     * 异常分发处理。
     *
     * <p>根据异常类型路由到对应的错误响应方法。</p>
     *
     * @param exchange 当前请求上下文
     * @param error    异常对象
     * @return 异步完成信号
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        // Sentinel 限流/熔断异常 → 429
        if (BlockException.isBlockException(error)) {
            return errorWriter.rateLimited(exchange);
        }
        // 路由未找到（404）→ 503
        if (error instanceof NotFoundException) {
            return errorWriter.dependencyUnavailable(exchange);
        }
        // 其他异常向上抛出，由 Spring 默认处理器处理
        return Mono.error(error);
    }

    /**
     * 异常处理器优先级。
     *
     * @return -2，确保在默认异常处理器之前执行
     */
    @Override
    public int getOrder() {
        return -2;
    }
}
