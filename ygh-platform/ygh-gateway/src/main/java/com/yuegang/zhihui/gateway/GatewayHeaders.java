package com.yuegang.zhihui.gateway;

/**
 * 网关自定义 HTTP 头常量定义。
 *
 * <p>统一管理网关在请求链路中传递的所有内部头信息，
 * 包括链路追踪 ID、用户上下文、客户端 IP 签名等。</p>
 *
 * <p>下游微服务通过读取这些头来获取网关注入的安全与追踪元数据。</p>
 *
 * @author yuegang-zhihui
 * @since 1.0.0
 */
interface GatewayHeaders {

    /**
     * 链路追踪 ID，贯穿整个请求生命周期
     */
    String TRACE_ID = "X-Trace-Id";

    /**
     * 请求 ID，标识单次 HTTP 请求
     */
    String REQUEST_ID = "X-Request-Id";

    /**
     * 用户 ID，由认证层注入
     */
    String USER_ID = "X-YGH-User-Id";

    /**
     * 用户角色列表，由认证层注入
     */
    String ROLES = "X-YGH-Roles";

    /**
     * 用户权限列表，由认证层注入
     */
    String PERMISSIONS = "X-YGH-Permissions";

    /**
     * 客户端真实 IP，由网关从连接层提取
     */
    String CLIENT_IP = "X-YGH-Client-IP";

    /**
     * 客户端 IP 时间戳，用于签名防重放
     */
    String CLIENT_IP_TIMESTAMP = "X-Client-IP-Timestamp";

    /**
     * 客户端 IP 签名，HMAC 防篡改
     */
    String CLIENT_IP_SIGNATURE = "X-YGH-Client-IP-Signature";

    /**
     * 用户上下文时间戳，用于签名防重放
     */
    String USER_CONTEXT_TIMESTAMP = "X-YGH-User-Context-Timestamp";

    /**
     * 用户上下文签名，HMAC 防篡改
     */
    String USER_CONTEXT_SIGNATURE = "X-YGH-User-Context-Signature";
}
