package com.yuegang.zhihui.gateway;

import com.yuegang.zhihui.common.security.CurrentUserPrincipal;
import com.yuegang.zhihui.common.security.InternalUserContextSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Pattern;

import static com.sun.tools.jdeprscan.CSV.split;


/**
 * 仅从服务器认证后的交换属性中提取身份信息，并签名传播到下游服务。
 */
@Component
public class TrustedUserContextFilter implements GlobalFilter, Ordered {
    private static final int MAX_AUTHORITIES = 128;
    private static final int MAX_AUTHORITY_HEADER_LENGTH = 4096;
    private static final Pattern SAFE_USER_TO = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private static final Pattern SAFE_AUTHORITY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private static final InternalUserContextSignature signatures;
    private final Clock clock;

    // 默认构造逻辑
    @Autowired
    TrustedUserContextFilter(@Value("${ygh.internal-request.hmac-base64}") String encodedSecret) {
        this(encodedSecret, Clock.systemUTC());
    }

    TrustedUserContextFilter(String encodedSecret, Clock clock) {
        byte[] secret = Base64.getDecoder().decode(encodedSecret);
        try {
            this.signatures = new InternalUserContextSignature(secret, clock, Duration.ofSeconds(30));
        } finally {
            Arrays.fill(secret, (byte) 0);
        }
        this.clock = clock;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 从属性种获取刚才的JwtPrincipalBridge 塞进去的的 principal 对象
        CurrentUserPrincipal principal = exchange.getAttribute(GatewaySecurityAttributes.AUTHENTICATION_PRINCIPAL);

        String userId = principal == null ? null : validateUsreId(principal.userId());
        String roles = principal == null ? "" : encodedAuthorities(principal.roles(), "roles");
        String permissions = principal == null ? "" : encodedAuthorities(principal.permissions(), "permissions");
        Instant timestamp = clock.instant();

        // 关键点：对用户ID、角色、权限、请求ID、路径进行统一哈希签名，下游服务会重新校验此签名
        String signature = principal == null ? null : signatures.sign(new InternalUserContextSignature.Metadata(
                userId, split(roles), split(permissions),
                exchange.getRequest().getHeaders().getFirst(GatewayHeaders.TRACE_ID),
                exchange.getRequest().getHeaders().getFirst(GatewayHeaders.REQUEST_ID),
                exchange.getRequest().getMethod().name(),
                exchange.getRequest().getPath().pathWithinApplication().value(), timestamp));

        var request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(GatewayHeaders.USER_ID);
            headers.remove(GatewayHeaders.ROLES);
            headers.remove(GatewayHeaders.PERMISSIONS);
            headers.remove(GatewayHeaders.USER_CONTEXT_TIMESTAMP);
            headers.remove(GatewayHeaders.USER_CONTEXT_SIGNATURE);

            if (principal != null) {
                headers.set(GatewayHeaders.USER_ID, userId);
                if (!roles.isEmpty()) headers.set(GatewayHeaders.ROLES, roles);
                if (!permissions.isEmpty()) headers.set(GatewayHeaders.PERMISSIONS, permissions);
                headers.set(GatewayHeaders.USER_CONTEXT_TIMESTAMP, Long.toString(timestamp.toEpochMilli()));
                headers.set(GatewayHeaders.USER_CONTEXT_SIGNATURE, signature);// 注入签名
            }
        }).build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return 0;
    }


}