package com.yuegang.zhihui.product.security;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.security.InternalUserContextSignature;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 类格式化调整
 * 产品管理权限校验器
 * 负责验证内部用户上下文签名并确认用户是否具有 ADMIN 角色
 */
public final class ProductAdminVerifier {

    // 内部用户上下文签名校验器
    private final InternalUserContextSignature s;

    /**
     * 构造函数
     *
     * @param k 用于签名校验的密钥字节数组
     */
    public ProductAdminVerifier(byte[] k) {
        // 初始化签名校验器，使用 UTC 时钟，设置签名有效期为 30 秒
        s = new InternalUserContextSignature(k, Clock.systemUTC(), Duration.ofSeconds(30));
    }

    /**
     * 辅助方法：获取必填的请求头
     *
     * @param r 请求对象
     * @param n 头名称
     * @return 请求头的值
     */
    private static String h(HttpServletRequest r, String n) {
        String v = r.getHeader(n);
        // 如果头信息不存在或为空白字符，则视为校验失败
        if (v == null || v.isBlank()) throw f();
        return v;
    }

    /**
     * 辅助方法：解析逗号分隔的字符串
     *
     * @param x 原始字符串
     * @return 字符串列表
     */
    private static List<String> v(String x) {
        // 如果输入为空，返回空列表，否则按逗号切割
        return x == null || x.isBlank() ? List.of() : List.of(x.split(","));
    }

    /**
     * 辅助方法：创建权限拒绝异常
     *
     * @return 业务异常实例
     */
    private static BusinessException f() {
        // 返回预定义的"权限不足"错误码异常
        return new BusinessException(ErrorCode.PERMISSION_DENIED);
    }

    /**
     * 执行校验逻辑
     *
     * @param r 当前 HTTP 请求对象
     */
    public void verify(HttpServletRequest r) {
        try {
            // 从请求头获取用户 ID
            String u = h(r, "X-YGH-User-Id");
            // 获取并解析角色列表（将逗号分隔的字符串转换为 List）
            var roles = v(r.getHeader("X-YGH-Roles"));
            // 获取并解析权限列表
            var perms = v(r.getHeader("X-YGH-Permissions"));

            // 构建内部用户上下文签名的元数据对象
            var m = new InternalUserContextSignature.Metadata(u,                      // 用户 ID
                    roles,                  // 角色列表
                    perms,                  // 权限列表
                    h(r, "X-Trace-Id"),     // 链路追踪 ID
                    h(r, "X-Request-Id"),   // 请求唯一 ID
                    r.getMethod(),          // HTTP 请求方法 (GET/POST 等)
                    r.getRequestURI(),      // 请求路径
                    Instant.ofEpochMilli(Long.parseLong(h(r, "X-YGH-User-Context-Timestamp"))) // 请求时间戳
            );
            // 执行签名校验，并同时检查用户角色中是否包含 "ADMIN"
            if (!s.verify(m, h(r, "X-YGH-User-Context-Signature")) || !roles.contains("ADMIN")) {
                // 校验失败（签名错误或非管理员），抛出权限拒绝异常
                throw f();
            }
        } catch (Exception e) {
            // 捕获任何异常（如格式解析错误等），统一按权限拒绝处理
            throw f();
        }
    }

}
