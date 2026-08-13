package com.yuegang.zhihui.system.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.security.CurrentUserPrincipal;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.system.application.AiProviderConfigService;
import com.yuegang.zhihui.system.security.InternalServiceVerifier;
import com.yuegang.zhihui.system.security.SystemTrustedUserContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController // 标识为一个 RESTful 风格的控制器组件
public class AiProviderConfigController { // 定义最终类: AI 供应商配置控制器
    private final AiProviderConfigService service; // 声明 AI 配置业务服务字段
    private final SystemTrustedUserContextResolver users; // 声明用户上下文解析器字段
    private final InternalServiceVerifier internalServices; // 声明内部服务验证器字段

    public AiProviderConfigController(AiProviderConfigService service, // 构造函数开始
                                      SystemTrustedUserContextResolver users, // 注入用户解析器
                                      InternalServiceVerifier internalServices) { // 注入服务器验证器
        this.service = service; // 初始化业务服务
        this.users = users; // 初始化用户解析器
        this.internalServices = internalServices; // 初始化服务验证器
    } // 构造函数结束

    @GetMapping("/api/v1/system/ai-provider-config") // 定义获取 AI 配置视图的接口
    public ApiResponse<AiProviderConfigView> view(HttpServletRequest request) { // 接收请求对象
        require(request, "ai:config:read"); // 调用私有方法检查是否有读取 AI 配置的权限
        return ApiResponse.success(service.view(), TraceIdResolver.resolve(request)); // 返回成功响应及追踪 ID
    } // 方法结束

    @GetMapping("/internal/v1/system/ai-provider-config") // 定义内部服务获取明文配置的接口
    public ApiResponse<InternalAiProviderConfig> internal(HttpServletRequest request) { // 接收请求对象
        String caller = request.getHeader("X-YGH-Service"); // 从请求头获取调用者服务名称
        if (!Set.of("ygh-ai-service", "ygh-search-service").contains(caller)) { // 校验调用者是否在白名单内
            throw new BusinessException(ErrorCode.UNAUTHENTICATED); // 若不在则抛出未认证异常
        } // 校验结束
        internalServices.verify(request, caller); // 验证内部服务的数字签名
        return ApiResponse.success(service.internal(), TraceIdResolver.resolve(request)); // 返回包含密钥的内部配置
    }

    private CurrentUserPrincipal require(HttpServletRequest request, String permission) { // 私有权限检查工具的方法
        CurrentUserPrincipal principal = users.resolve(request); // 解析受信任的用户身份信息
        if (!principal.roles().contains("ADMIN") && !principal.permissions().contains(permission)) { // 如果不是管理员且无特定权限
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);//抛出权限拒绝异常
        }
        return principal;
    } // 方法结束

}
