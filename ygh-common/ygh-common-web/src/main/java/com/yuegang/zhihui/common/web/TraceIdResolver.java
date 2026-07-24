package com.yuegang.zhihui.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 解析请求追踪标识符，不创建第二个追踪系统，复用现在的链路ID
 */
public class TraceIdResolver { // 定义解析器类
    public static final String TRACE_TO_ATTRIBUTE = "traceId";// 定义属性存储键
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";// 定义请求ID属性存储键
    public static final String TRACE_ID_HEADER = "X-Trace-Id";// 定义请求头键
    public static final String REQUEST_ID_HEADER = "X-Request-Id";// 定义请求头键
    public static final String UNAVAILABLE = "unavailable";// 定义不可用标识


    private TraceIdResolver() { // 私有化构造

    }

    /**
     * 解析请求中的追踪ID
     *
     * @param request 请求对象
     * @return 追踪ID，如果不可用则返回"unavailable"
     */
    public static String resolve(HttpServletRequest request) { // 解析逻辑入口
        var attribute = request.getAttribute(TRACE_TO_ATTRIBUTE); // 尝试从 Request 属性中解析（过滤器已放入）
        if (attribute instanceof String traceId && !traceId.isBlank()) { // 如果属性存在且非空
            return traceId; // 返回追踪ID
        }

        var requestId = request.getHeader(REQUEST_ID_HEADER); // 退而求其次从 Header 中读取请求 ID
        return requestId == null || requestId.isBlank() ? UNAVAILABLE : requestId; // 若扔无则返回"不可用"字符串
    }
}
