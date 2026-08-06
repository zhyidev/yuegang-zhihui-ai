package com.yuegang.zhihui.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest( // 定义公共记录类：创建部门请求 DTO r
                                       String parentId, // 属性:父级部门ID(可为空，表示顶级部门)
                                       @NotBlank @Size(max = 32) String code, // 校验:部门的编码不能为空最大32个字符
                                       @NotBlank @Size(max = 100) String name, // 校验:部门的名称不能为空月最大100个字符
                                       int sortOrder // 顺序
) {
}