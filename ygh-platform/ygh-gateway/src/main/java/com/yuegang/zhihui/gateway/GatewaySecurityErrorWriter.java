package com.yuegang.zhihui.gateway;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.core.ErrorCode;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import tools.jackson.databind.ObjectMapper;

/**
 * 网关安全错误响应写入器。
 *
 * <p>统一处理网关层的安全异常，将错误信息以 JSON 格式写入 HTTP 响应。
 * 所有方法返回 {@link Mono#empty()} 表示已提交的响应不再处理。</p>
 *
 * <p>支持的错误类型：</p>
 * <ul>
 *   <li>401 未认证</li>
 *   <li>403 无权限 / 上传路径被拒绝</li>
 *   <li>411 缺少 Content-Length</li>
 *   <li>413 请求体过大</li>
 *   <li>429 限流</li>
 *   <li>503 下游依赖不可用</li>
 * </ul>
 *
 * @author yuegang-zhihui
 * @since 1.0.0
 */
@Component
public class GatewaySecurityErrorWriter {

    /** JSON 序列化器，用于将错误响应对象转为字节数组 */
    private final ObjectMapper objectMapper;

    /**
     * 构造器。
     *
     * @param objectMapper Jackson ObjectMapper 实例
     */
    GatewaySecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 401 未认证 —— 用户未提供有效凭证。
     *
     * @param exchange 当前请求上下文
     * @return 异步完成信号
     */
    Mono<Void> unauthenticated(ServerWebExchange exchange) {
        return write(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED);
    }

    /**
     * 403 无权限 —— 用户已认证但无访问权限。
     *
     * @param exchange 当前请求上下文
     * @return 异步完成信号
     */
    Mono<Void> accessDenied(ServerWebExchange exchange) {
        return write(exchange, HttpStatus.FORBIDDEN, ErrorCode.PERMISSION_DENIED);
    }

    /**
     * 429 限流 —— 请求频率超过限制。
     *
     * <p>响应头中会携带 {@code Retry-After: 1} 提示客户端等待时间。</p>
     *
     * @param exchange 当前请求上下文
     * @return 异步完成信号
     */
    Mono<Void> rateLimited(ServerWebExchange exchange) {
        if (!exchange.getResponse().isCommitted()) {
            exchange.getResponse().getHeaders().set("Retry-After", "1");
        }
        return write(exchange, HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMITED);
    }

    /**
     * 503 依赖不可用 —— 下游服务不可达或超时。
     *
     * @param exchange 当前请求上下文
     * @return 异步完成信号
     */
    Mono<Void> dependencyUnavailable(ServerWebExchange exchange) {
        return write(exchange, HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.DEPENDENCY_UNAVAILABLE);
    }

    /**
     * 413 请求体过大 —— 请求 Body 超过网关限制。
     *
     * @param exchange 当前请求上下文
     * @return 异步完成信号
     */
    Mono<Void> payloadTooLarge(ServerWebExchange exchange) {
        return write(exchange, HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.VALIDATION_ERROR);
    }

    /**
     * 403 上传路径被拒绝 —— 请求的路径不在上传白名单中。
     *
     * @param exchange 当前请求上下文
     * @return 异步完成信号
     */
    Mono<Void> uploadPathRejected(ServerWebExchange exchange) {
        return write(exchange, HttpStatus.FORBIDDEN, ErrorCode.PERMISSION_DENIED);
    }

    /**
     * 411 缺失 Content-Length —— 上传请求未携带 Content-Length 头。
     *
     * @param exchange 当前请求上下文
     * @return 异步完成信号
     */
    Mono<Void> lengthRequired(ServerWebExchange exchange) {
        return write(exchange, HttpStatus.LENGTH_REQUIRED, ErrorCode.VALIDATION_ERROR);
    }

    /**
     * 内部通用错误响应写入逻辑。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>如果响应已提交，则跳过（幂等保护）；</li>
     *   <li>获取当前请求的 TraceId 用于问题排查；</li>
     *   <li>构建 {@link ApiResponse} 失败对象并序列化为 JSON；</li>
     *   <li>设置 HTTP 状态码和响应头；</li>
     *   <li>将 JSON 字节写入响应体。</li>
     * </ol>
     *
     * @param exchange 当前请求上下文
     * @param status   要返回的 HTTP 状态码
     * @param errorCode 业务错误码
     * @return 异步完成信号（已提交响应）
     */
    private Mono<Void> write(ServerWebExchange exchange, HttpStatus status, ErrorCode errorCode) {
        // 响应已提交则跳过，防止重复写入
        if (exchange.getResponse().isCommitted()) return Mono.empty();

        // 获取当前请求的 TraceId，用于问题排查
        String traceId = exchange.getAttributeOrDefault(
                GatewaySecurityAttributes.TRACE_ID, "unavailable");

        // 构造标准失败响应体并序列化为 JSON 字节数组
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(
                    ApiResponse.failure(errorCode, null, traceId));
        } catch (Exception e) {
            // 序列化失败时返回空响应，避免异常扩散
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }

        // 设置 HTTP 状态码和响应头
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setContentLength(body.length);

        // 将 JSON 字节写入响应体
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
