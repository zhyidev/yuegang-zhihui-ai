package com.yuegang.zhihui.system.security;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.security.CurrentUserPrincipal;
import com.yuegang.zhihui.common.security.InternalUserContextSignature;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

// 该类用于在微服务内部从 HTTP 确认中还原出受信任的用户上下文信息（包括ID、角色和权限），并校验其签名。
public final class SystemTrustedUserContextResolver { // 定义最终类，受信任用户上下文解析器
    private final InternalUserContextSignature signatures; // 声明用户上下文签名操作对象

    public SystemTrustedUserContextResolver(byte[] s, Clock c) { // 构造函数：注入签名密钥和时钟
        // 初始化签名器类，设置 30 秒容差，确保下游服务收到的用户信息是由网关加签且未超时的
        signatures = new InternalUserContextSignature(s, c, Duration.ofSeconds(30));
    } // 构造函数结束

    private static String h(HttpServletRequest r, String n) { // 辅助方法：获取必填头信息
        String x = r.getHeader(n); // 读取 Header
        if (x == null || x.isBlank()) throw f(); // 缺失则报错
        return x; // 返回值
    } // h 方法结束

    private static List<String> v(String x) { // 辅助方法：将逗号分隔的字符串安全地转换为列表
        return x == null || x.isBlank() ? List.of() : List.of(x.split(",")); // 为空返回空列表，否则执行分割
    } // v 方法结束

    private static BusinessException f() { // 辅助方法:统一常工厂
        return new BusinessException(ErrorCode.UNAUTHENTICATED); // 返回未认证异常
    }

    public CurrentUserPrincipal resolve(HttpServletRequest r) { // 核心方法：从请求中解析用户信息
        try { // 开启尝试块
            String u = h(r, "X-YGH-User-Id"); // 获取可信的用户 ID
            var roles = v(r.getHeader("X-YGH-Roles"));// 获取并分角色列表
            var perms = v(r.getHeader("X-YGH-Permissions")); // 获收#分列表
            // 构造验证元数据，包含用户信息、链路ID(Trace/Request)以及网关生成此信息时的时间戳
            var m = new InternalUserContextSignature.Metadata(u, roles, perms, h(r, "X-YGH-Trace-Id"), h(r, "X-Request-Id"), r.getMethod(), r.getRequestURI(), Instant.ofEpochMilli(Long.parseLong(h(r, "X-YGH-User-Timestamp"))));
            // 执行签名校验，如果 X-YGH-User-Context-Signature 不正确，说明用户信息在中途被篡改或伪造
            if (!signatures.verify(m, h(r, "X-YGH-User-Context-Signature"))) throw f();
// 校验通过后，构造并返回同一个包含用户、角色和权限的 CurrentUserPrincipal 对象
            return new CurrentUserPrincipal(u, new LinkedHashSet<>(roles), new LinkedHashSet<>(perms));
        } catch (BusinessException e) { // 捕获已知业务异常
            throw e; // 直接抛出
        } catch (RuntimeException e) { // 捕获解析异常（如数字格式化错、空指针等）
            throw e; // 封装并抛出未认证异常
        }
    } // resolve 方法结束

}
