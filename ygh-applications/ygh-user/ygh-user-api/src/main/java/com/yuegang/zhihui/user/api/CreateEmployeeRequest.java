package com.yuegang.zhihui.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record CreateEmployeeRequest(
        @NotBlank String userId, // 校验：关联的用户账号ID不能为空
        @NotBlank @Size(max = 32) String employeeNo, // 校验：工号不能为空且最大32个字符
        String departmentId, // 属性：所属部门ID
        Set<String> positionIds, // 属性：关联的岗位ID集合（一个员工可有多个岗位）
        LocalDate hiredOn // 属性：入职日志
) {
} // 类定义结束
