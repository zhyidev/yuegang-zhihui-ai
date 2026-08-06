package com.yuegang.zhihui.user.security;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.security.InternalServiceSignature;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * 该类用于验证内部微服务之间的调用安全性（服务间鉴权）
 */
public final class UserInternalServiceVerifier { // 定义内部服务验证器类
    private final InternalServiceSignature signatures; // 声明内部服务签名工具

    public UserInternalServiceVerifier(byte[] k) { // 构造函数：传入密钥
        signatures = new InternalServiceSignature(k, Clock.systemUTC(), Duration.ofSeconds(30)); // 初始化签名器，使用UTC时钟，30秒过期
    }

    private static String h(HttpServletRequest r, String n) { // 获取请求头的简写方法
        String v = r.getHeader(n); // 获取请求头值
        if (v == null || v.isBlank()) throw denied(); // 如果为空抛出拒绝异常
        return v; // 返回值
    }

    private static BusinessException denied() { // 定义拒绝访问异常的便捷方法
        return new BusinessException(ErrorCode.UNAUTHENTICATED); // 返回未授权错误码对应的异常
    }

    public void verify(HttpServletRequest r) { // 验证请求的方法
        try { // 开启尝试块
            String service = h(r, "X-YGH-Service"); // 获取调用方服务名称头
            Instant time = Instant.ofEpochMilli(Long.parseLong(h(r, "X-YGH-Service-Timestamp"))); // 获取并转换请求时间戳
            var m = new InternalServiceSignature.Metadata(service, r.getMethod(), r.getRequestURI(), time); // 构建服务签名元数据
            if (!signatures.verify(m, h(r, "X-YGH-Service-Signature"))) throw denied(); // 校验签名，不通过则抛出拒绝异常
        } catch (Exception e) { // 捕获任何异常
            throw denied(); // 统一转换为拒绝访问异常
        }
    }
}