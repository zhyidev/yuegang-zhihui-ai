package com.yuegang.zhihui.common.security;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yuegang.zhihui.common.core.BusinessException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResourceAccessGuardTest { // 资源访问守卫 (ResourceAccessGuard) 单元测试类

    private static final String MANAGE_ANY_ADDRESS =
            "user:address:manage:any"; // 定义测试用"跨用户管理地址"特权点常量

    private final ResourceAccessGuard guard = new ResourceAccessGuard(); // 实例化待测试的守卫对象

    /**
     * BusinessException 的错误载体属于 common-core 契约：最终对外稳定值必须是 PERMISSION_DENIED。
     */
    private static String stableErrorCode(BusinessException exception) { // 读取错误码工具方法
        return exception.errorCode().code(); // 直接读取错误码字符串
    }

    @Test
    void shouldAllowOwnerToAccessOwnResource() { // 测试方法：应当允许资源拥有者访问属于自己的资源
        CurrentUserPrincipal principal =
                principal("user-1001", Set.of("CUSTOMER"), Set.of()); // 创建普通客户用户
        assertThatCode(
                        () ->
                                guard.requireOwnerOrPermission(
                                        principal, // 登录主体
                                        "user-1001", // 资源拥有者ID
                                        MANAGE_ANY_ADDRESS // 跨所有者特权点
                                        ))
                .doesNotThrowAnyException(); // 验证拥有者顺利通过，不抛出异常
    }

    @Test
    // 标记为 JUnit5 测试方法
    void
            shouldAllowAdministratorOnlyWhenExplicitByPassPermissionIsPersent() { // 测试方法：仅当管理员显式具备特权点时才允许越权访问
        CurrentUserPrincipal principal =
                principal(
                        "admin-1001", // 用户ID
                        Set.of("ADMIN"), // 角色
                        Set.of(MANAGE_ANY_ADDRESS) // 显式授予了跨所有者特权点
                        ); // 实例化完成

        assertThatCode(
                        () ->
                                guard.requireOwnerOrPermission( // 执行守卫检查：管理员访问 user-1001 的资源
                                        principal, // 登录主体
                                        "user-1001", // 资源拥有者ID
                                        MANAGE_ANY_ADDRESS // 跨所有者特权点
                                        ))
                .doesNotThrowAnyException(); // 验证具备显式特权点的管理员顺利放行
    }

    @Test
    // 标记为 JUnit5 测试方法
    void shouldNotTreatAdministratorRoleAsImplicitOwnershipBypass() { // 测试方法：不能将单独的 ADMIN
        // 角色当作隐式越权许可
        CurrentUserPrincipal principal =
                principal("admin-1001", Set.of("ADMIN"), Set.of()); // 创建仅有 ADMIN 角色但无特权点的用
        BusinessException exception =
                assertThrows(
                        BusinessException.class, // 预期异常类型
                        () ->
                                guard.requireOwnerOrPermission(
                                        principal, "user-1001", MANAGE_ANY_ADDRESS), // 执行越权检查
                        "ADMIN role alone must not bypass ownership" // 断言失效时的提示
                        );

        assertThat(stableErrorCode(exception))
                .isEqualTo("PERMISSION_DENIED"); // 验证最终返回的错误码字符串中稳定为 PERMISSION_DENIED
    }

    private CurrentUserPrincipal principal(
            String userId, Set<String> roles, Set<String> permissions) { // 工厂辅助方法，创建用户主题
        return new CurrentUserPrincipal(userId, roles, permissions); // 返回实例
    }

    @Test
    // 标记为 JUnit5 测试方法
    void
            shouldRejectNonOwnerWithoutPermissionUsingStableBusinessError() { // 测试方法：拒绝既非拥有者又无特权的用户，并返回稳定的错误码
        CurrentUserPrincipal principal =
                principal(
                        "user-2002", // 用户ID
                        Set.of("CUSTOMER"), // 角色
                        Set.of("user:address:read") // 仅有读取自己地址的普通权限
                        ); // 实例化完成

        BusinessException exception =
                assertThrows( // 断言放行越权操作时抛出异常
                        BusinessException.class, // 预期异常类型
                        () ->
                                guard.requireOwnerOrPermission(
                                        principal,
                                        "user-1001",
                                        MANAGE_ANY_ADDRESS), // user-2002 尝试修改 user-1001 的资源，理应被拦截
                        "Cross-owner access must require a explicit permission" // 断言失败提示
                        );

        assertThat(stableErrorCode(exception))
                .isEqualTo("PERMISSION_DENIED"); // 验证错误码稳定为 PERMISSION_DENIED
    }
}
