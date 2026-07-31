package com.yuegang.zhihui.user.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.user.api.CreateDepartmentRequest;
import com.yuegang.zhihui.user.api.DepartmentView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/** 管理组织架构（部门、职位、员工）的业务逻辑 */
public final class OrganizationService { // 定义组织架构服务类
    private JdbcTemplate jdbc; // 声明 JDBC 模板
    private final TransactionTemplate tx; // 声明事务模板
    private final UserIdGenerator ids; // 声明 ID 雪花生成器

    private OrganizationService(DataSource source, UserIdGenerator ids) { // 构造函数初始化
        this.jdbc = new JdbcTemplate(source); // 根据数据源创建 JDBC 模板
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(source)); // 创建事务模板
        this.ids = ids; // 注入 ID 生成器
    }

    public DepartmentView createDepartment(CreateDepartmentRequest request) { // 创建部门
        try {
            long id = ids.nextId(); // 获取分布式 ID
            Long parent = optional(request.parentId()); // 解析可选的父部门 ID
        }
    }

    private static Long optional(String value) {
        return value == null || value.isBlank() ? null : positive(value); // 辅助处理可选 ID
    }

    private static long positive(String value) { // 辅助校验正整数 ID
        try {
            long id = Long.parseLong(value);
            if (id == 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}