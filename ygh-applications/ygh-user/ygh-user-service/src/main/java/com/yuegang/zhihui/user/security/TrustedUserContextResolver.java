package com.yuegang.zhihui.user.security;

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


/**
 * 该类负责解析并验证来自网关或内部服务的用户信息头（包含HMAC签名校验）
 */
public final class TrustedUserContextResolver { // 定义受信任的内部用户上下文解析器类
    private final InternalUserContextSignature signatures; // 声明内部用户上下文签名校验器

    public TrustedUserContextResolver(byte[] secret, Clock clock) { // 构造函数：传入密钥和时钟
        this.signatures = new InternalUserContextSignature(secret, clock, Duration.ofSeconds(30)); // 初始化签名校验器，设置30秒有效期
    }

    private static String header(HttpServletRequest request, String name) { // 私有辅助方法：获取必填请求头
        String value = request.getHeader(name); // 从请求中读取指定名称的头信息
        if (value == null || value.isBlank()) throw unauthenticated(); // 如果为空或空白，抛出未授权异常
        return value; // 返回头信息
    }

    private static List<String> values(String encoded) { // 私有辅助方法：解析逗号分隔的字符串为列表
        return encoded == null || encoded.isBlank() ? List.of() : List.of(encoded.split(",", -1)); // 为空返回空列表，否则按逗号分隔
    }

    private static BusinessException unauthenticated() { // 快捷构建未授权异常的方法
        return new BusinessException(ErrorCode.UNAUTHENTICATED);
    }

    public CurrentUserPrincipal resolve(HttpServletRequest request) { // 解析请求中的用户信息
        try { // 开始尝试解析逻辑
            String userId = header(request, "X-YGH-User-Id"); // 从请求头获取用户 ID

            List<String> roles = values(request.getHeader("X-YGH-Roles")); // 获取并解析用户角色列表
            List<String> permissions = values(request.getHeader("X-YGH-Permissions")); // 获取并解析用户权限列表
            Instant timestamp = Instant.ofEpochMilli(Long.parseLong(header(request, "X-YGH-User-Context-Timestamp"))); // 解析请求头中的时间戳
            var metadata = new InternalUserContextSignature.Metadata(userId, roles, permissions, // 构建签名元数据对象
                header(request, "X-Trace-Id"), header(request, "X-Request-Id"),
                request.getMethod(), // 包含追踪ID、请求ID、请求方法
                request.getRequestURI(), timestamp); // 包含请求URI和时间戳
            if (!signatures.verify(metadata, header(request, "X-YGH-User-Context-Signature")))
                throw unauthenticated(); // 校验签名，若失败则
            long numericId = Long.parseLong(userId); // 将用户ID转换为长整型以进行格式校验
            if (numericId <= 0) throw unauthenticated(); // 校验用户ID必须大于0
            return new CurrentUserPrincipal(userId, new LinkedHashSet<>(roles), new LinkedHashSet<>(permissions)); // 返回构建好的当前用户主体对象
        } catch (BusinessException expected) {
            throw expected;
        } // 如果是已知的业务异常，直接向上抛出
        catch (RuntimeException malformed) {
            throw unauthenticated();
        } // 如果发生运行时异常（如格式错误），视为认证失败
    }
}
