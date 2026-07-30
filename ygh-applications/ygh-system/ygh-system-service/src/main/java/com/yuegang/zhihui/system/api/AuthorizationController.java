package com.yuegang.zhihui.system.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.security.CurrentUserPrincipal;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.system.application.AuthorizationService;
import com.yuegang.zhihui.system.security.SystemTrustedUserContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

//授权管理控制器
@RestController // 声明 REST 控制器
@RequestMapping("/api/v1/system/users") // 统一定义根路径为用户系统接口
public final class AuthorizationController { // 定义最终类: 授权控制器
    private final AuthorizationService service; // 声明授权应用服务
    private final SystemTrustedUserContextResolver users; // 声明用户上下文解析器

    public AuthorizationController(AuthorizationService s, SystemTrustedUserContextResolver u) { // 构造函数注入
        service = s; // 赋值服务
        users = u; // 赋值解析器
    } // 构造函数结束

    @GetMapping("/{userId}/authorities") // 获取指定用户权限快照的接口
    public ApiResponse<AuthoritySnapshot> snapshot(@PathVariable String userId, HttpServletRequest r) { // 接收用户参数和请求
        CurrentUserPrincipal p = users.resolve(r); // 解析操作者身份
        if (!p.userId().equals(userId) && !p.roles().contains("ADMIN")) // 如果不是本人且不是管理员
            throw new BusinessException(ErrorCode.PERMISSION_DENIED); // 拒绝访问
        return ApiResponse.success(service.snapshot(userId), TraceIdResolver.resolve(r)); // 返回用户的权限快照
    } // 方法结束

    @PutMapping("/{userId}/roles") // 分配用户权限的接口
    public ApiResponse<AuthoritySnapshot> assign(@PathVariable String userId, @Valid @RequestBody AssignRolesRequest body, HttpServletRequest r) { // 请求参数接受
        CurrentUserPrincipal p = users.resolve(r); // 解析当前操作人
        if (!p.roles().contains("ADMIN") && !p.permissions().contains("system:rbac:write")) // 必须拥有管理员角色或 RBAC 写入权限
            throw new BusinessException(ErrorCode.PERMISSION_DENIED); // 权限不足报错
        return ApiResponse.success(service.assign(userId, body, Long.parseLong(p.userId())), TraceIdResolver.resolve(r)); // 执行分配并返回新快照
    } // 方法结束
} // 类定义结束