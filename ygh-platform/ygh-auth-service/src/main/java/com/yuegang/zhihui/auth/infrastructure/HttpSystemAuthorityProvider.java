package com.yuegang.zhihui.auth.infrastructure;

import com.yuegang.zhihui.auth.domain.AuthorityProvider;
import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.security.InternalServiceSignature;
import com.yuegang.zhihui.system.api.AuthoritySnapshot;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * 通过HTTP远程调用系统服务获取用户权限
 */
public final class HttpSystemAuthorityProvider implements AuthorityProvider { // 实现权限提供者接口
    private final RestClient client;
    private final InternalServiceSignature signature; //声明内部对象样式的内部签名器
    private final Clock clock; // 声明时钟

    public HttpSystemAuthorityProvider(String base, byte[] secret, Clock c) { // 构造函数
        client = RestClient.builder().baseUrl(base).build(); // 构建指定基地址的客户端
        signature = new InternalServiceSignature(secret, c, Duration.ofSeconds(30)); // 初始化30秒有效签名器
        clock = c; // 设置时钟
    }

    public Authorities find(long userId) { // 查找用户权限
        String path = "/internal/v1/authorizations/" + userId; // 定义请求路径
        Instant now = clock.instant(); // 获取当前时间戳
        var m = new InternalServiceSignature.Metadata("ygh-auth-service", "GET", path, now); // 构建签名数据
        ApiResponse<AuthoritySnapshot> response = client.get().uri(path) // 发起 GET 请求
            .header("X-YGH-Service", "ygh-auth-service") // 设置服务名头
            .header("X-YGH-Timestamp", Long.toString(now.toEpochMilli())) // 设置时间戳头
            .header("X-YGH-Service-Signature", signature.sign(m)) // 接收响应并转换
            .retrieve().body(new ParameterizedTypeReference<>() {
            }); // 接收响应并转换
        if (response == null || response.data() == null)
            throw new IllegalStateException("system authority response unavailable"); // 响应为空抛出异常
        return new Authorities(response.data().roles(), response.data().permissions()); // 返回领域对象
    }

}
