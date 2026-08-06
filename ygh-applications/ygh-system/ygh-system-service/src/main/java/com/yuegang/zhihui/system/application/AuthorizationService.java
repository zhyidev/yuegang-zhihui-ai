package com.yuegang.zhihui.system.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.system.api.AssignRolesRequest;
import com.yuegang.zhihui.system.api.AuthoritySnapshot;
import com.yuegang.zhihui.system.domain.AuthorizationRepository;

// 该服务作为应用层入口，负责处理用户角色的分配与查询。
public class AuthorizationService { // 定义最终类：授权服务
    private final AuthorizationRepository repository; // 声明授权仓储接口

    public AuthorizationService(AuthorizationRepository r) { // 构造函数
        repository = r; // 注入仓储实现
    } // 构造函数结束

    private static long id(String s) {
        try { // 开启转换
            long v = Long.parseLong(s); // 解析长整型
            if (v <= 0) throw new NumberFormatException(); // 强制要求正数
            return v; // 返回结束
        } catch (NumberFormatException e) { // 捕获格式异常
            throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 抛出参数校验业务异常
        }
    }

    public AuthoritySnapshot assign(String user, AssignRolesRequest r, long operator) { // 方法：为用户分配角色
        if (r.reason() != null && r.reason().length() > 500)
            throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 校验原因长度
        return repository.replaceRoles(id(user), r.version(), r.roleCodes(), operator, r.reason()).orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_CONFLICT)); // 调用仓储层的角色替换方法，并处理版本冲突
    } // 方法结束

    public AuthoritySnapshot snapshot(String user) { // 方法：获取指定用户的权限快照
        return repository.snapshot(id(user)); // 调用仓储层的获取快照
    }
}