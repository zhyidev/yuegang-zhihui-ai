package com.yuegang.zhihui.system.application;

import com.yuegang.zhihui.system.api.RoleView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// 该服务负责角色与权限的增删改查管理，包含复杂的事务编排。
public class RoleAdministrationService { // 定义最终类：角色管理服务
    private final JdbcTemplate jdbc; // 声明 JDBC 模板
    private final TransactionTemplate tx; // 声明事务模板

    public RoleAdministrationService(DataSource dataSource) { // 构造函数
        jdbc = new JdbcTemplate(dataSource); // 初始化 JDBC 模板
        tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource)); // 初始化事务模板
    } // 构造结束

    public List<RoleView> roles() { // 方法：查询所有角色列表
        return jdbc.query("SELECT id, name,enabled,version FROM system_role ORDER BY built_in DESC,code", (r, n) -> role(r.getLong(1), r.getString(2),
                r.getString(3), r.getBoolean(4), r.getLong(5))); // 执行 SQL 查询并映射结果
    } // 结束

    private RoleView role(long id, String code, String name, boolean enabled, long version) { // 私有方法：深度组装角色（含权限点列表）
        Set<String> permissions = new LinkedHashSet<>(jdbc.queryForList("SELECT p.code FROM system_permission p JOIN system_role_permission rp " +
                "ON p.id = rp.permission_id WHERE rp.role_id = ? ORDER BY p.code", String.class, id)); // 联表查询角色权限点列表
        return new RoleView(Long.toString(id), code, name, enabled, version, permissions); // 返回视图对象
    } // 结束

    private static long next() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE; //   1 / 生成 64 位随机数作为角色 ID
    } // 结束
}