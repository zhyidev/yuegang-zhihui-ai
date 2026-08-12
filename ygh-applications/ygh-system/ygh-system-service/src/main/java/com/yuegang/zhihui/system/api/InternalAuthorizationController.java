package com.yuegang.zhihui.system.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.system.application.AuthorizationService;
import com.yuegang.zhihui.system.security.InternalServiceVerifier;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 内部授权控制器
@RestController // 声明 REST 控制器
@RequestMapping("/internal/v1/authorizations") // 定义仅供内部微服务调用的根路径
public final class InternalAuthorizationController { // 定义最终类
    private final AuthorizationService service; // 声明授权服务
    private final InternalServiceVerifier verifier; // 声明服务间校验器

    public InternalAuthorizationController(AuthorizationService s, InternalServiceVerifier v) { // 构造函数
        service = s; // 注入服务
        verifier = v; // 注入校验器
    } // 构造函数结束

    @GetMapping("/{userId}") // 内部服务查询用户权限的接口
    public ApiResponse<AuthoritySnapshot> snapshot(@PathVariable String userId, HttpServletRequest r) { // 接收用户 ID 和请求对象
        verifier.verify(r, "ygh-auth-service"); // 强制校验请求必须来自认证服务（且签名正确）
        return ApiResponse.success(service.snapshot(userId), TraceIdResolver.resolve(r)); // 返回权限快照给内部服务
    } // 方法结束
} // 类结束
