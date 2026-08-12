package com.yuegang.zhihui.common.security;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;

/**
 * Enforces resource ownership with an explicit permission for cross-owner access.
 * A role name such as ADMIN never bypasses ownership by itself.
 */
public final class ResourceAccessGuard {
    public void requireOwnerOrPermission( // 检查是否拥有者是否拥有权限 no usages
                                          CurrentUserPrincipal principal, // 当前操作的用户主体
                                          String ownerUserId, // 被访问资源所属的用户 ID
                                          String crossOwnerPermission // 允许跨主体访问所需的指定权限码
    ) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED); // 抛出未认证异常
        }

        boolean owner = ownerUserId != null && ownerUserId.equals(principal.userId()); // 逻辑判断：资源所属 ID 是否匹配当前用户 ID
        boolean explicitlyAllowed = principal.hasPermission(crossOwnerPermission); // 逻辑判断：用户是否拥有管理员性质的跨越权限
        if (!owner && !explicitlyAllowed) {
            throw new PermissionDeniedException(); // 抛出权限拒绝异常
        }
    }

    public <I> void requireOwnerOrPermission( // 方法：基于拥有者检查器接口的泛型版本 no usages
                                              CurrentUserPrincipal principal, // 认证主体
                                              I resourceId, // 资源的泛型 ID
                                              ResourceOwnershipChecker<I> ownershipChecker, // 外部传入的拥有者逻辑判定接口
                                              String crossOwnerPermission // 跨资源权限码
    ) {
        if (principal == null) { // 登录检验
            throw new BusinessException(ErrorCode.UNAUTHENTICATED); // 拦截未登录
        }
        if (ownershipChecker == null) { // 检查器不能为空
            throw new IllegalArgumentException("ownershipChecker must not be null"); // 参数错误报告
        }
        boolean owner = ownershipChecker.isOwner(principal, resourceId); // 调用业务逻辑：询问检查器该用户是否拥有该资源
        boolean explicitlyAllowed = principal.hasPermission(crossOwnerPermission); // 检查跨资源管理权限
        if (!owner && !explicitlyAllowed) { // 拦截条件：非管理员且无授权
            throw new PermissionDeniedException(); // 抛出拒绝异常
        }
    }


}
