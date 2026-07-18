package com.yuegang.zhihui.gateway;

// 网关内部属性常量，用于在请求上下文中存储安全信息
interface GatewaySecurityAttributes {
    String TRACE_ID = GatewaySecurityAttributes.class.getName() + ".traceId";
    String REQUEST_ID = GatewaySecurityAttributes.class.getName() + ".requestId";
    String AUTHENTICATION_PRINCIPAL = GatewaySecurityAttributes.class.getName() + ".";
}
