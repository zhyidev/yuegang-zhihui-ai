package com.yuegang.zhihui.system.api;

import jakarta.validation.constraints.*;

import java.util.Set;

/**
 * 新增或更新角色请求 DTO
 */
public record UpsertRoleRequest(@NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{0,63}") String code, // 角色编码：非空，全大写蛇形，限64
                                @NotBlank @Size(max = 100) String name, // 角色名称：非空且限100位
                                @NotNull Set<@Pattern(regexp = "[A-Za-z][A-Za-z0-9:_-]{0,127}") String> permissions,
                                // 关联权限编码集：非空，每个便哈都需要符合通用编码正则
                                boolean enabled, @PositiveOrZero long version) { // 是否启用、版本号校验
}