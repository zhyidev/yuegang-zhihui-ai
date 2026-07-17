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


/**
 * 仅从服务器认证后的交换属性中提取身份信息，并签名传播到下游服务。
 */
@Component
public class TrustedUserContextFilter implements GlobalFilter, Ordered {
    // 限制用户上下文中可传播的角色/权限数量，避免头部膨胀。
    private static final int MAX_AUTHORITIES = 128;
    // 限制编码后的角色/权限头长度，避免超过请求头安全阈值。
    private static final int MAX_AUTHORITY_HEADER_LENGTH = 4096;
    // 用户 ID 的安全白名单模式，限制只允许可传播的字符集。
    private static final Pattern SAFE_USER_TO = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    // 角色/权限项的安全白名单模式，限制内部上下文只接受受控值。
    private static final Pattern SAFE_AUTHORITY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");
    private final InternalUserContextSignature signatures;
    private final Clock clock;

    // 从配置中读取 HMAC 密钥，构造受信任用户上下文签名器。
    @Autowired
    TrustedUserContextFilter(@Value("${ygh.internal-request.hmac-base64}") String encodedSecret) {
        this(encodedSecret, Clock.systemUTC());
    }

    // 便于测试/替换时钟的构造入口。
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
        // 读取前序过滤器放入 exchange 属性中的认证主体；如果没有则视为未认证。
        CurrentUserPrincipal principal = exchange.getAttribute(GatewaySecurityAttributes.AUTHENTICATION_PRINCIPAL);

        // 将用户 ID、角色、权限转为可写入下游请求头的字符串形式。
        String userId = principal == null ? null : validateUserId(principal.userId());
        String roles = principal == null ? "" : encodedAuthorities(principal.roles(), "roles");
        String permissions = principal == null ? "" : encodedAuthorities(principal.permissions(), "permissions");
        // 使用统一时钟生成用户上下文签名时间戳。
        Instant timestamp = clock.instant();

        // 关键点：对用户ID、角色、权限、请求ID、路径进行统一哈希签名，下游服务会重新校验此签名
        // 将用户上下文与链路信息一起签名，供下游服务校验请求是否经过可信网关。
        String signature = principal == null ? null : signatures.sign(new InternalUserContextSignature.Metadata(
                userId, split(roles), split(permissions),
                exchange.getRequest().getHeaders().getFirst(GatewayHeaders.TRACE_ID),
                exchange.getRequest().getHeaders().getFirst(GatewayHeaders.REQUEST_ID),
                exchange.getRequest().getMethod().name(),
                exchange.getRequest().getPath().pathWithinApplication().value(), timestamp));

        // 先移除旧的用户上下文头，再在存在认证主体时注入新的可信头。
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
        // 当前与认证桥接过滤器同级排序，后续若需要稳定链路顺序可再调整。
        return 0;
    }


}