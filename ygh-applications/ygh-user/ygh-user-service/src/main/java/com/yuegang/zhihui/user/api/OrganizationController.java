package com.yuegang.zhihui.user.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.security.CurrentUserPrincipal;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.user.application.OrganizationService;
import com.yuegang.zhihui.user.security.TrustedUserContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 该类提供组织架构管理（部门、职位、员工）的管理端 API
 */
@RestController // 标识为管理器
@RequestMapping("/api/v1/organization") // 设置组织架构模块的基础路径
public class OrganizationController { // 定义组织架构控制器类
    private final OrganizationService service; // 注入业务服务
    private final TrustedUserContextResolver users; // 注入安全解析器

    public OrganizationController(OrganizationService service, TrustedUserContextResolver users) { // 构造函数注入业务服务
        this.service = service;
        this.users = users;
    }

    private static <T> ApiResponse<T> ok(T data, HttpServletRequest request) { // 封装成功响应的辅助方法
        return ApiResponse.success(data, TraceIdResolver.resolve(request));
    }

    @GetMapping("/departments")
        // 查询部门列表
    ApiResponse<List<DepartmentView>> departments(HttpServletRequest request) { // 查询部门列表,需要读取权限
        require(request, "organization:read");
        return ok(service.departments(), request);
    }

    @PostMapping("/departments")
    ApiResponse<DepartmentView> createDepartment(@Valid @RequestBody CreateDepartmentRequest body, HttpServletRequest request) { // 创建部门，need write permission
        require(request, "organization:write");
        return ok(service.createDepartment(body), request);
    }

    @PostMapping("/positions")
    ApiResponse<PositionView> createPosition(@Valid @RequestBody CreatePositionRequest body, HttpServletRequest request) { // 创建职位，需要写权限
        require(request, "organization:write");
        return ok(service.createPosition(body), request);
    }

    @GetMapping("/positions")
    ApiResponse<List<PositionView>> positions(HttpServletRequest request) { // 查询职位列表，需要读权限
        require(request, "organization:read");
        return ok(service.positions(), request);
    }

    @GetMapping("/employees")
    ApiResponse<List<EmployeeView>> employees(HttpServletRequest request) { // 查询员工列表，需要员工读权限
        require(request, "employee:read");
        return ok(service.employees(), request);
    }

    @PostMapping("/employees")
    ApiResponse<EmployeeView> createEmployee(@Valid @RequestBody CreateEmployeeRequest body, HttpServletRequest request) { // 创建员工，需要员工写权限
        require(request, "employee:write");
        return ok(service.createEmployee(body), request);
    }

    @PutMapping("/employees/{id}/positions")
    ApiResponse<EmployeeView> positions(@PathVariable String id, @RequestBody Set<String> positions, HttpServletRequest request) {
        require(request, "employee:write");
        return ok(service.replacePositions(id, positions), request);
    }

    @PutMapping("/employees/{id}/status/{status}")
    ApiResponse<EmployeeView> status(@PathVariable String id, @PathVariable String status, HttpServletRequest request) { // 更改员工在职状态，需要写权限
        require(request, "employee:write");
        return ok(service.changeStatus(id, status), request);
    }

    private void require(HttpServletRequest request, String permission) { // 私有权限检查辅助方法
        CurrentUserPrincipal principal = users.resolve(request); // 解析当前用户信息
        if (!principal.hasRole("ADMIN") && !principal.hasPermission(permission))
            throw new BusinessException(ErrorCode.PERMISSION_DENIED); // 非管理员且无特定权限则抛出拒绝访问
    }

}