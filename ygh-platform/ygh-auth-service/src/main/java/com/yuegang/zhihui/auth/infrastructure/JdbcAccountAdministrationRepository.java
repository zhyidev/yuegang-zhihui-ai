package com.yuegang.zhihui.auth.infrastructure;

import com.yuegang.zhihui.auth.domain.AccountAdministrationRepository;
import com.yuegang.zhihui.auth.domain.AccountStatus;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Types;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * 账号状态管理（激活/禁用）的JDBC实现，包含事务和审计
 */
public final class JdbcAccountAdministrationRepository implements AccountAdministrationRepository { // 实现账号行政管理接口
    private final DataSource dataSource; // 实现账号行政管理接口

    public JdbcAccountAdministrationRepository(DataSource dataSource) { // 构造函数注入
        this.dataSource = dataSource;
    }

    @Override
    public Optional<StatusChange> changeStatus(long userId, AccountStatus status, long expectedVersion, long operationUserId, String reason) {
        try (var connection = dataSource.getConnection()) { // 获取连接
            connection.setAutoCommit(false); // 开启事务（禁止自动提交）
            try {
                long accountId; // 声明账号 ID
                try (var find = connection.prepareStatement("SELECT id FROM auth_account WHERE user_id=? FOR UPDATE")) { // 设定信息并锁定
                    find.setLong(1, userId); // 设置参数
                    try (var rows = find.executeQuery()) { // 执行查询
                        if (!rows.next()) {
                            connection.rollback();
                            return Optional.empty();
                        } // 未找到则回滚并返回
                        accountId = rows.getLong(1); // 提取ID
                    }
                }
                // 更新账号状态：重置错误计数、清除锁定、增加版本号
                try (var update = connection.prepareStatement("UPDATE auth_account SET status=?,failed_login_count=0,locked_until=NULL,version=version+1 WHERE id=? AND version=?")) {
                    update.setString(1, status.name()); // 设置新状态名
                    update.setLong(2, accountId); // 设置ID
                    update.setLong(3, expectedVersion); // 乐观锁：设置预期版本
                    if (update.executeUpdate() != 1) {
                        connection.rollback();
                        return Optional.empty();
                    } // 更新行数不为1代表版本冲突，回滚
                }
                // 插入审计日志
                try (var audit = connection.prepareStatement("INSERT INTO auth_account_admin_audit(account_id,user_id,operator_user_id,action,reason) VALUES(?,?,?,?,?)")) {
                    audit.setLong(1, accountId); // 设置账号ID
                    audit.setLong(2, userId); // 设置用户ID
                    audit.setLong(3, operationUserId); // 设置操作人ID
                    audit.setString(4, status.name()); // 设置动作
                    if (reason == null || reason.isBlank()) audit.setNull(5, Types.VARCHAR); // 原因可选
                    else audit.setString(5, reason.trim()); // 填写原因
                    audit.executeUpdate(); // 执行审计插入
                }
                StatusChange result; // 声明返回结果
                try (var read = connection.prepareStatement("SELECT status,version,updated_at FROM auth_account WHERE id=?")) { // 读取更新后的最终状态
                    read.setLong(1, accountId); // setting
                    try (var rows = read.executeQuery()) {
                        rows.next(); // 移动到第一行
                        // 组装状态变更记录，转换时间戳为 UTF OffsetDateTime
                        result = new StatusChange(accountId, userId, AccountStatus.valueOf(rows.getString(1)), rows.getLong(2),
                                rows.getTimestamp(3).toLocalDateTime().atOffset(ZoneOffset.UTC));
                    }
                }

                connection.commit();
                return Optional.of(result); // 提交事务并返回结果
            } catch (SQLException failure) {
                connection.rollback();
                throw failure;
            } // 异常则回滚并抛出
            finally {
                connection.setAutoCommit(true);
            }//恢复自动提交状态
        } catch (SQLException failure) {
            throw new AccountSecurityPersistenceException("failed to administer account", failure); // 抛出自定义持久化异常
        }
    }
}
