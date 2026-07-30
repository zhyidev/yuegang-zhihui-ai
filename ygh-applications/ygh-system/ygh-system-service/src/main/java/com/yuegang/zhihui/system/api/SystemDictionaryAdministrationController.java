package com.yuegang.zhihui.system.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.system.application.SaveDictionaryItemRequest;
import com.yuegang.zhihui.system.application.SystemDictionaryAdministrationService;
import com.yuegang.zhihui.system.security.SystemTrustedUserContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 声明 REST 控制器
@RequestMapping("/api/v1/system/dictionaries/admin") // 定义专门用于管理员维护字典的根路径
public class SystemDictionaryAdministrationController { // 定义类
    private final SystemDictionaryAdministrationService service; // 字典管理具体业务服务
    private final SystemTrustedUserContextResolver users; // 身份解析器

    public SystemDictionaryAdministrationController(SystemDictionaryAdministrationService s, SystemTrustedUserContextResolver u) { // 构造注入
        service = s; // 赋值
        users = u; // 赋值
    } // 结束

    @GetMapping
        // 获取字典所有层级结构的接口（用于管理后台列表）
    ApiResponse<List<DictionaryAdminView>> all(HttpServletRequest r) {
        admin(r);
        return ok(service.all(), r); // 返回管理员视图列表
    }

    @PutMapping("/{code}")
        // 更新或新增字典类型的接口（如：性别分类）
    ApiResponse<DictionaryAdminView> type(@PathVariable String code, @Valid @RequestBody SaveDictionaryTypeRequest b, HttpServletRequest r) {
        admin(r); // 权限校验
        if (!code.equals(b.code())) throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 路径编号与内容编码不一致则报错
        return ok(service.saveType(b), r); // 保存分类并返回
    } // 结束

    @PutMapping("/{code}/items/{key}")
        // 更新或新增字典具体项的接口（如：男/女）
    ApiResponse<DictionaryAdminView> item(@PathVariable String code, @PathVariable String key, @Valid @RequestBody SaveDictionaryItemRequest b, HttpServletRequest r) {
        admin(r); // 权限校验
        if (!key.equals(b.key())) throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 键一致性检查
        return ok(service.saveItem(code, b), r); // 保存项并返回整个分类的新状态
    } // 结束

    private void admin(HttpServletRequest r) { // 私有方法: 强制要求管理员权限
        var p = users.resolve(r); // 解析当前操作者
        if (!p.roles().contains("ADMIN") && !p.permissions().contains("system:rbac:write")) // 判断管理员角色或字典写权限
            throw new BusinessException(ErrorCode.PERMISSION_DENIED); // 校验失败抛出 403
    } // 结束

    private static <T> ApiResponse<T> ok(T x, HttpServletRequest r) { // 封装成功地响应结果工具方法
        return ApiResponse.success(x, TraceIdResolver.resolve(r)); // 构造带追踪的 ID 成功
    } // 结束
}