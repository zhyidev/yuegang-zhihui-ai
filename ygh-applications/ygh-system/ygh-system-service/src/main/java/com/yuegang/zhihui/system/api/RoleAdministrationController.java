package com.yuegang.zhihui.system.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.security.CurrentUserPrincipal;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.system.application.RoleAdministrationService;
import com.yuegang.zhihui.system.security.SystemTrustedUserContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 角色权限行政管理控制器
@RestController // 声明为 REST 控制器
@RequestMapping("/api/v1/system/admin") // 统一定义管理后台路径
public class RoleAdministrationController { // 定义最终类：角色行政管理
    private final RoleAdministrationService service; // 角色管理业务逻辑
    private final SystemTrustedUserContextResolver users; // 身份解析逻辑

    public RoleAdministrationController(RoleAdministrationService service, SystemTrustedUserContextResolver users) { // 构造函数
        this.service = service; // 初始化
        this.users = users; // 初始化
    } // 构造结束

    @GetMapping("/roles")
        // 获取系统中所有角色清单
    ApiResponse<List<RoleView>> roles(HttpServletRequest request) { // 授权请求
        admin(request); // 校验管理员权限
        return ok(service.roles(), request); // 返回角色列表
    } // 结束

    @GetMapping("/permissions") // 获取系统中所有权限点清单
    ApiResponse<List<PermissionView>> permissions(HttpServletRequest request) { // 接收请求
        admin(request); // 权限检查
        return ok(service.permissions(), request); // 返回权限列表
    } // 结束

    @PutMapping("/permissions/{code}")
    ApiResponse<PermissionView> permission(@PathVariable String code, @Valid @RequestBody UpsertPermissionRequest body, HttpServletRequest request) { // 路径变量
        admin(request); // 权限检查
        return ok(service.savePermission(code, body), request); // 记录并返回
    } // 结束

    @PutMapping("/roles/{code}") // 保存或更新特定编码的角色
    ApiResponse<RoleView> role(@PathVariable String code, @Valid @RequestBody UpsertRoleRequest body, HttpServletRequest request) { // 路径变量
        admin(request); // 权限检查
        return ok(service.saveRole(code, body), request); // 保存并返回结果
    } // 结束


    private void admin(HttpServletRequest request) { // 私有方法: 强制要求管理员权限
        CurrentUserPrincipal user = users.resolve(request); // 解析当前操作者
        if (!user.roles().contains("ADMIN") && !user.permissions().contains("system:rbac:write")) // 判定角色或特定权限点
            throw new BusinessException(ErrorCode.PERMISSION_DENIED); // 校验失败抛出 403
    } // 结束

    private static <T> ApiResponse<T> ok(T data, HttpServletRequest request) { // 封装成功地响应结果工具方法
        return ApiResponse.success(data, TraceIdResolver.resolve(request)); // 生成统一响应格式
    } // 结束
}