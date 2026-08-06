package com.yuegang.zhihui.user.api;

import java.time.LocalDate;
import java.util.Set;

public record EmployeeView( // 定义公共记录类：员工信息展示 DTO
                            String id, // 属性：员工唯一主键ID
                            String userId, // 属性：关联的用户账号ID
                            String employeeNo, // 属性：员工工号
                            String departmentId, // 属性：所属部门ID
                            Set<String> positionIds, // 属性：当前拥有的所有岗位ID集合
                            String status, // 属性：在职状态（如在职、离职、试用期）
                            LocalDate hiredOn, // 属性：入职时间
                            long version // 属性：乐观锁版本号
) {
} // 类定义结束