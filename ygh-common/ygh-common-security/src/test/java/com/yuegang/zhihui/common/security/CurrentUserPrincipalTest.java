package com.yuegang.zhihui.common.security;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CurrentUserPrincipalTest { // 当前用户主体对象（CurrentUserPrincipal）单元测试类

    @Test
        // 标记为 JUnit5 测试方法
    void shouldKeepStringUserIdAndDefensivelyCopyAuthorities() { // 测试方法：验证应保持字符串类型的用户ID并对权限/角色集合进行防御性拷贝
        Set<String> roles = new LinkedHashSet<>(Set.of("CUSTOMER")); // 创建可变的角色集合，初始包含 CUSTOMER
        Set<String> permissions = new LinkedHashSet<>(Set.of("user:address:read")); // 创建可变的权限集合，初始包含地址读取权限

        CurrentUserPrincipal principal = new CurrentUserPrincipal("9007199254740993", roles, permissions); // 实例化用户主体记录对象
        roles.add("ADMIN"); // 尝试向外部传入的原角色集合中添加新角色 ADMIN
        permissions.add("user:address:manage:any"); // 尝试向外部传入的原权限集合中添加新权限

        assertThat(principal.userId()).isEqualTo("9007199254740993"); // 验证用户ID正确保留为大数字字符串格式（防JS精度丢失）
        assertThat(principal.roles()).containsExactly("CUSTOMER"); // 验证用户内部角色集合未受到外部集合修改影响，实现防御性拷贝
        assertThat(principal.permissions()).containsExactly("user:address:read"); // 验证主体内部权限集合未受到外部集合修改影响
        assertThatThrownBy(() -> principal.roles().add("ADMIN")) // 验证尝试直接对主体内部返回的角色集合进行修改时
                .isInstanceOf(UnsupportedOperationException.class); // 断言必定抛出不可修改集合的异常
        assertThatThrownBy(() -> principal.permissions().add("user:address:manage:any")) // 验证尝试直接对主体内部返回的权限集合进行修改时
                .isInstanceOf(UnsupportedOperationException.class); // 断言必定抛出不可修改集合的异常
    }

    @Test    // 标记为 JUnit5 测试方法
    void shouldMatchRolesAndPermissionsExactly() {  // 测试方法：验证角色与权限的对比必须精准匹配
        CurrentUserPrincipal principal = new CurrentUserPrincipal(
                "user-1001",  // 用户ID
                Set.of("KNOWLEDGE_REVIEWER"),  // 角色集合
                Set.of("knowledge:document:review", "user:address:read")  // 权限集合
        ); // 实例化完成

        assertThat(principal.hasRole("KNOWLEDGE_REVIEWER")).isTrue();  // 验证完全匹配的大写角色返回 true
        assertThat(principal.hasRole("knowledge_reviewer")).isFalse();  // 验证小写角色因大小写不一致返回 false
        assertThat(principal.hasPermission("knowledge:document:review")).isTrue();  // 验证完全匹配的权限字符串返回 true
        assertThat(principal.hasPermission("knowledge:document")).isFalse();  // 验证缺少后半段的模糊匹配返回 false
        assertThat(principal.hasPermission("KNOWLEDGE:DOCUMENT:REVIEW")).isFalse();  // 验证全大写权限字符串因大小写不一致返回 false
    }
}