package com.yuegang.zhihui.gateway;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * 网关请求守卫过滤器 —— 限制请求体大小并校验上传路径。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>区分普通请求和上传（multipart）请求，应用不同的大小限制；</li>
 *   <li>仅允许配置的白名单路径接收 multipart 上传；</li>
 *   <li>通过 Content-Length 头快速拦截超大请求；</li>
 *   <li>对 Chunked 传输（无 Content-Length）流式读取并检测超限。</li>
 * </ol>
 *
 * <p>执行顺序：{@link Ordered#HIGHEST_PRECEDENCE}，最早执行。</p>
 *
 * @author yuegang-zhihui
 * @since 1.0.0
 */
@SuppressWarnings("NullableProblems")
public class GatewayRequestGuardFilter implements WebFilter, Ordered {

    /**
     * 可配置的最大请求体上限（100MB），防止配置错误导致内存溢出
     */
    static final long MAX_CONFIGURABLE_BYTES = 100L * 1024 * 1024;

    /**
     * 普通请求的最大 Body 大小（字节）
     */
    private final long requestMaxBytes;

    /**
     * 上传请求的最大 Body 大小（字节）
     */
    private final long uploadMaxBytes;

    /**
     * 允许上传的路径白名单
     */
    private final Set<String> uploadPaths;

    /**
     * 安全错误响应写入器
     */
    private final GatewaySecurityErrorWriter errorWriter;

    /**
     * 构造器。
     *
     * @param requestMaxBytes 普通请求最大字节数
     * @param uploadMaxBytes  上传请求最大字节数
     * @param uploadPaths     允许上传的路径集合
     * @param errorWriter     错误响应写入器
     */
    GatewayRequestGuardFilter(long requestMaxBytes, long uploadMaxBytes,
                              Set<String> uploadPaths, GatewaySecurityErrorWriter errorWriter) {
        this.requestMaxBytes = requestMaxBytes;
        this.uploadMaxBytes = uploadMaxBytes;
        this.uploadPaths = uploadPaths;
        this.errorWriter = errorWriter;
    }

    /**
     * 重新封装 Request，使下游过滤器能再次读取 Body 字节流。
     *
     * <p>Spring WebFlux 的 Body 默认只能读取一次，
     * 该方法通过装饰器模式保留了已读取的 Buffer 供下游复用。</p>
     *
     * @param exchange   当前请求上下文
     * @param chain      过滤器链
     * @param bufferBody 已读取的请求体 Buffer（可能为 null）
     * @return 异步完成信号
     */
    private static Mono<Void> replay(ServerWebExchange exchange, WebFilterChain chain,
                                     DataBuffer bufferBody) {
        var guardedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public Flux<DataBuffer> getBody() {
                if (bufferBody == null) return Flux.empty();
                return Flux.defer(() -> Flux.just(DataBufferUtils.retain(bufferBody)));
            }
        };

        Mono<Void> result = Mono.defer(() ->
            chain.filter(exchange.mutate().request(guardedRequest).build()));

        if (bufferBody == null) return result;

        // 逻辑处理完后释放缓冲区内存
        return result.doFinally(ignored -> DataBufferUtils.release(bufferBody));
    }

    /**
     * 核心过滤逻辑。
     *
     * <p>判断请求类型（普通/上传），应用对应的大小限制，
     * 超限或非法上传路径则返回错误响应。</p>
     *
     * @param exchange 当前请求上下文
     * @param chain    过滤器链
     * @return 异步完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 判断是否为 multipart 上传请求
        boolean multipart = exchange.getRequest().getHeaders().getContentType() != null
            && MediaType.MULTIPART_FORM_DATA.isCompatibleWith(
            exchange.getRequest().getHeaders().getContentType());

        String path = exchange.getRequest().getPath().pathWithinApplication().value();

        // 仅当请求为 POST 且路径在白名单中时，才允许上传
        boolean allowedUpload = multipart
            && HttpMethod.POST.equals(exchange.getRequest().getMethod())
            && uploadPaths.contains(path);

        // 非法上传路径 → 403
        if (multipart && !allowedUpload) {
            return errorWriter.uploadPathRejected(exchange);
        }

        // 读取 Content-Length 进行初步判断
        long contentLength = exchange.getRequest().getHeaders().getContentLength();

        // 上传接口禁止不传 Content-Length
        if (allowedUpload && contentLength < 0) {
            return errorWriter.lengthRequired(exchange);
        }

        // 超限直接拒绝（Content-Length 已知的情况）
        long limit = allowedUpload ? uploadMaxBytes : requestMaxBytes;
        if (contentLength > limit) {
            return errorWriter.payloadTooLarge(exchange);
        }

        // 有明确长度且未超限，直接放行
        if (contentLength >= 0) {
            return chain.filter(exchange);
        }

        // Chunked 传输（长度未知）：流式读取，超限即抛异常
        return DataBufferUtils.join(exchange.getRequest().getBody(), Math.toIntExact(limit))
            .singleOptional()
            .flatMap(buffer -> replay(exchange, chain, buffer.orElse(null)))
            .onErrorResume(DataBufferLimitException.class,
                ignored -> errorWriter.payloadTooLarge(exchange));
    }

    /**
     * 过滤器执行顺序。
     *
     * @return HIGHEST_PRECEDENCE，最先执行
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
