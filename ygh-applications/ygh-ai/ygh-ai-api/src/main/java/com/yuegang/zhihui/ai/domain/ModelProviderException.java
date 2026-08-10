package com.yuegang.zhihui.ai.domain;

import java.util.Locale;

/**
 * 外部模型提供方返回的安全、结构化错误，不包含凭据和原始请求
 */
public final class ModelProviderException extends RuntimeException {
    private final int httpStatus; // 保存 HTTP 状态码
    private final String providerCode; // 保存供应商定义的错误码
    private final String providerMessage; // 保存供应商原始错误信息

    // 构造函数：初始化异常信息并对敏感数据进行脱敏处理
    public ModelProviderException(int httpStatus, String providerCode, String providerMessage, Throwable cause) {
        super("model provider request failed", cause); // 调用父类构造函数
        this.httpStatus = httpStatus;
        this.providerCode = safe(providerCode, "UNKNOWN"); // 脱敏处理错误码
        this.providerMessage = safe(providerMessage, "未返回错误说明"); // 脱敏处理详细说明
    }

    /**
     * 核心安全方法：使用正则表达式擦除字符串中的 API Key、Bearer Token 和账号 ID 等敏感信息
     */
    private static String safe(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback; // 空值返回默认退路字符串

        String sanitized = value
                .replaceAll("[\\r\\n\\t]", " ") // 将换行和制表符替换为空格
                .replaceAll("(?i)bearer\\s+[a-zA-Z0-9._-]+", "Bearer [REDACTED]") // 隐藏 HTTP Bearer 认证信息
                .replaceAll("(?i)\\bsk-[a-zA-Z0-9_-]+", "[REDACTED]") // 隐藏常见的 sk- 开头的 API 密钥
                .replaceAll("(?i)account\\s+\\d+", "account [REDACTED]") // 隐藏供应商账号数字 ID
                .replaceAll("(?i)request\\s*id\\s*:\\s*[a-zA-Z0-9_-]+", "Request id: [REDACTED]"); // 隐藏请求追踪 ID
        // 限制返回长度，防止过大的错误载荷导致聂村或目录溢出
        return sanitized.length() > 300 ? sanitized.substring(0, 300) : sanitized;
    }

    // 获取HTTP状态码
    public int httpStatus() {
        return httpStatus;
    }

    // 获取脱敏后的供应商错误码
    public String providerCode() {
        return providerCode;
    }

    // 获取脱敏后的供应商错误说明
    public String providerMessage() {
        return providerMessage;
    }

    /**
     * 将技术性的错误码转换为用户友好的中文提示（针对豆包/火山方舟大模型优化）
     */
    public String userMessage() {
        String normalized = providerCode.toLowerCase(Locale.ROOT); // 统一转为小写进行匹配

        // 匹配：模型未开通情况
        if (normalized.contains("model not open")) {
            return "豆包模型尚未开通，请先在火山方舟控制台开通当前对话模型，或在后台填写已开通的 Endpoint ID";
        }

        // 匹配：模型 ID 或端点不存在
        if (normalized.contains("model notfound") || normalized.contains("end counterpoint")) {
            return "豆包未找到当前模型，请在后台填写此 API key 已开通的模型 ID 或 Endpoint ID";
        }

        // 匹配：鉴权失败或无权限
        if (httpStatus == 401 || httpStatus == 403 || normalized.contains("auth")) {
            return "豆包 API key 无效或没有当前模型的调用权限";
        }

        // 匹配：欠费或配额不足
        if (normalized.contains("balance") || normalized.contains("quota") || normalized.contains("overdue")) {
            return "豆包账户额度或余额不足，请在火山方舟控制台检查计费状态";
        }

        // 匹配：请求频率过快（限流）
        if (httpStatus == 429 || normalized.contains("rate limit")) {
            return "豆包请求频率超限，请稍后重试";
        }
        // 默认返回通用的错误提示
        return "豆包调用失败 HTTP" + httpStatus + ",错误码" + providerCode + ")";
    }
}