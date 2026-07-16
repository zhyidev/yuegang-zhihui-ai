package com.yuegang.zhihui.gateway;

/**
 * 网关安全属性键常量定义。
 *
 * <p>定义在 {@link ServerWebExchange # getAttributes()} 中使用的键名，
 * 用于在请求处理链路中传递安全上下文（TraceId、RequestId、认证主体等）。</p>
 *
 * <p>使用类全限定名作为前缀，确保键名唯一，避免与 Spring 内部属性冲突。</p>
 *
 * @author yuegang-zhihui
 * @since 1.0.0
 */
interface GatewaySecurityAttributes {

    /** 链路追踪 ID 的属性键 */
    String TRACE_ID = GatewaySecurityAttributes.class.getName() + ".traceId";

    /** 请求 ID 的属性键 */
    String REQUEST_ID = GatewaySecurityAttributes.class.getName() + ".requestId";

    /** 认证主体（用户信息）的属性键 */
    String AUTHENTICATION_PRINCIPAL = GatewaySecurityAttributes.class.getName() + ".principal";
}
