package com.yuegang.zhihui.gateway;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.util.Set;
import java.util.regex.Pattern;


/*大包与非法路径保护*/
// 网关请求保护过滤器占位类，实际逻辑可能在其他代码中补充
@SuppressWarnings("NullableProblems")
public class GatewayRequestGuardFilter implements WebFilter, Ordered {


    static final  long MAX_CONFIGURABLE_BYTES = 100L * 1024 *1024; //100MB 上限
    private static final Pattern SAFE_UPLOAD_BYTES = Pattern.compile("/api/v1/[A-Za-z0-9/_-]+");

    private final long requestMaxBytes;
    private final long uploadMaxBytes;
    private final Set<String> uploadPaths;
    private final GatewaySecurityErrorWriter errorWriter;

    GatewayRequestGuardFilter(long requestMaxBytes, long uploadMaxBytes, Set<String> uploadPaths,GatewaySecurityErrorWriter errorWriter) {
        this.requestMaxBytes = requestMaxBytes;
        this.uploadMaxBytes = uploadMaxBytes;
        this.uploadPaths = uploadPaths;
        this.errorWriter = errorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 判断是否为Multipart上传格式
        boolean multipart = exchange.getRequest().getHeaders().getContentType() != null && MediaType.MULTIPART_FORM_DATA.isCompatibleWith(exchange.getRequest().getHeaders().getContentType());

        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        // 只有post 白名单路径内才允许上传
        boolean allowedUpload = multipart && HttpMethod.POST.equals(exchange.getRequest().getMethod()) && uploadPaths.contains(path);

        // 如果非白名单，直接拦截返回 403
        if (multipart && !allowedUpload) {
            return errorWriter.uploadPathRejected(exchange);
        }

        // 读取 Content Length
        long contentLength = exchange.getRequest().getHeaders().getContentLength();
        if (allowedUpload && contentLength < 0) {
            return errorWriter.lengthRequired(exchange); //上传接口禁止不传 length
        }

        long limit = allowedUpload ? uploadMaxBytes : requestMaxBytes;
        if (contentLength > limit) {
            return errorWriter.payloadTooLarge(exchange); // 报文过大
        }

        // 如果带有明确长度的常规请求，直接放行
        if (contentLength >= 0) {
            return chain.filter(exchange);
        }

        // 如果是Chunked 传输（长度未知），需求流式读取并计算，超过limit，抛出异常
        return DataBufferUtils.join(exchange.getRequest().getBody(),Math.toIntExact(limit))
                .singleOptional()
                .flatMap(buffer -> replay(exchange,chain,buffer.orElse(null)))
                .onErrorResume(DataBufferLimitException.class,ignored -> errorWriter.payloadTooLarge(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 15;
    }


    // 辅助方法 重新封装Request，使下游再次读取Body字节流
    private static Mono<Void> replay(ServerWebExchange exchange, WebFilterChain chain, DataBuffer bufferBody) {
        var GuardeRequest = new ServerHttpRequestDecorator(exchange.getRequest()){
            @Override
            public Flux<DataBuffer> getBody(){
                if (bufferBody == null) return Flux.empty();
                return Flux.defer(() -> Flux.just(DataBufferUtils.retain(bufferBody)));
            }
        };
        Mono<Void> result = Mono.defer(() -> chain.filter(exchange.mutate().request(GuardeRequest).build()));
        if(bufferBody == null) return result;
        // 逻辑处理完后释放缓冲内存
        return result.doFinally(ignored -> DataBufferUtils.release(bufferBody));
    }


}
