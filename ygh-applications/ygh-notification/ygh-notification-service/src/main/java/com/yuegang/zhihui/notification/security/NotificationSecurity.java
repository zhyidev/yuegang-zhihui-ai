package com.yuegang.zhihui.notification.security;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.security.InternalServiceSignature;
import com.yuegang.zhihui.common.security.InternalUserContextSignature;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 该类负责解析并验证内部请求的用户上下文和系统间调用的签名。
 */
public final class NotificationSecurity { // 定义安全校验类
    private final InternalUserContextSignature users; // 声明内部用户上下文签名的验证器
    private final InternalServiceSignature services; // 声明内部服务调用签名验证器

    public NotificationSecurity(byte[] k) { // 构造函数：注入 HMAC 密钥
        // 初始化用户签名校验器，使用 UTC 时钟，有效期 30 秒
        this.users = new InternalUserContextSignature(k, Clock.systemUTC(), Duration.ofSeconds(30));
// 初始化服务签名校验器，使用 UTC 时钟，有效期 30 秒
        this.services = new InternalServiceSignature(k, Clock.systemUTC(), Duration.ofSeconds(30));
    }

    public long user(HttpServletRequest r) { // 从请求中直接解析并返回用户 ID
        return context(r).user; // 调用 context 方法解析并提取用户字段
    }

    public long require(HttpServletRequest r, String permission) { // 检查用户是否具备特定权限
        var c = context(r); // 解析用户上下文
        // 如果既不是管理员角色，也不具备传入的权限点，则抛出权限不足异常
        if (!c.roles.contains("ADMIN") && !c.permissions.contains(permission))
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        return c.user; // 校验通过，返回用户ID
    }


    public void service(HttpServletRequest r) { // 验证内部服务间的请求签名
        try {
            String n = h(r, "X-YGH-Service"); // 获取调用方服务名称
            // 解析调动方传来的时间戳
            Instant t = Instant.ofEpochMilli(Long.parseLong(h(r, "X-YGH-Service-Timestamp")));
            // 组装元数据并使用签名工具进行哈希验证
            if (!services.verify(new InternalServiceSignature.Metadata(n, r.getMethod(), r.getRequestURI(), t), h(r, "X-YGH-Service-Signature")))
                throw f();
        } catch (Exception e) { // 捕获解析或签名不匹配异常
            throw f();
        }
    }

    private Context context(HttpServletRequest r) { // 内部方法：解析受信任的用户上下文
        try {
            String u = h(r, "X-YGH-User-Id"); // 获取用户 ID 请求头
            var roles = v(r.getHeader("X-YGH-Roles")); // 获取解析角色列表
            var perms = v(r.getHeader("X-YGH-Permissions"));// 获取并解析权限列表
            // 构建用户上下文元数据
            var m = new InternalUserContextSignature.Metadata(u, roles, perms, h(r, "X-Trace-Id"), h(r, "X-Request_Id"), r.getMethod(), r.getRequestURI(), Instant.ofEpochMilli(Long.parseLong(h(r, "X-YGH-User-Context-Timestamp"))));
            // 验证用户上下文签名是否由受信任的网关生成
            if (!users.verify(m, h(r, "X-YGH-User-Context-Signature"))) throw f(); //返回解析后的上下文记录
            return new Context(Long.parseLong(u), roles, perms);
        } catch (BusinessException e) {// 其他解析异常视为非法请求
            throw f();
        }
    }

    private record Context(Long user, List<String> roles, List<String> permissions) {
    } // 定义内部上下文数据载体

    private static String h(HttpServletRequest r, String n) {
        String v = r.getHeader(n); // 获取指定名称的 Header
        if (v == null || v.isBlank()) throw f(); // 如果头信息缺失或为空白，抛出未授权异常
        return v;
    }

    private static List<String> v(String x) {// 快捷解析逗号分隔字符串为列表的方法
        return x == null || x.isBlank() ? List.of() : List.of(x.split(",")); // 分隔 and return list
    }

    private static BusinessException f() { // 统一定义未授权异常
        return new BusinessException(ErrorCode.UNAUTHENTICATED);
    }
}