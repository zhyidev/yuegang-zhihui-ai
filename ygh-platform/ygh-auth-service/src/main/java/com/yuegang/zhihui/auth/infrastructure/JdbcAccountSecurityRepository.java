package com.yuegang.zhihui.auth.infrastructure;

import com.yuegang.zhihui.auth.domain.AccountAccessState;
import com.yuegang.zhihui.auth.domain.AccountSecurityRepository;
import com.yuegang.zhihui.auth.domain.AccountSecuritySnapshot;
import com.yuegang.zhihui.auth.domain.AccountStatus;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Optional;

/**
 * 提供账号安全快照查询和原子状态更新
 */
public final class JdbcAccountSecurityRepository implements AccountSecurityRepository { // 实现账号安全接口
    private static final String FIND_SQL = """
            SELECT id, status, failed_login_count, locked_until, version
            FROM auth_account WHERE id = ?
            """; // 查询账号关键安全属性的SQL

    private static final String UPDATE_SQL = """
            UPDATE auth_account
            SET failed_login_count = ?, locked_until = ?, version = version + 1
            WHERE id = ? AND version = ?
            """; // 乐观锁更新安全状态的SQL

    private final DataSource dataSource; // 声明数据源

    public JdbcAccountSecurityRepository(DataSource dataSource) { // 构造函数
        this.dataSource = java.util.Objects.requireNonNull(dataSource, "datasource must not be null"); // 注入并检查非空
    }

    @Override
    public Optional<AccountSecuritySnapshot> findById(long accountId) {
        try (var connection = dataSource.getConnection(); // 获取连接
             var statement = connection.prepareStatement(FIND_SQL)) { // 预处理SQL
            statement.setLong(1, accountId); // 设置参数
            try (var rows = statement.executeQuery()) { // 执行
                if (!rows.next()) {
                    return Optional.empty(); // 没找到返回空
                }
                Timestamp lockedUntil = rows.getTimestamp("locked_until"); // 获取锁定时间
                var state = new AccountAccessState( // 构建访问状态领域对象
                        AccountStatus.valueOf(rows.getString("status")), // 状态枚举
                        rows.getInt("failed_login_count"), // 失败计数
                        Optional.ofNullable(lockedUntil).map(Timestamp::toInstant)); // 锁定时间 (转为 Instant)
                return Optional.of(new AccountSecuritySnapshot( // 返回快照
                        rows.getLong("id"), state, rows.getLong("version"))); // 包含版本号
            }
        } catch (SQLException exception) { // 捕获异常
            throw new AccountSecurityPersistenceException("failed to read account security state", exception); // 抛出异常
        }
    }

    @Override
    public boolean compareAndSetAccessState(long accountId, long expectedVersion, AccountAccessState newState) { // 比较并交换（原子更新）状态
        try (var connection = dataSource.getConnection(); // 获取连接
             var statement = connection.prepareStatement(UPDATE_SQL)) { // 预处理更新
            statement.setInt(1, newState.failedLoginCount()); // 设置新失败次数
            if (newState.lockedUntil().isPresent()) { // 如果有锁定时间
                statement.setTimestamp(2, Timestamp.from(newState.lockedUntil().orElseThrow())); // 设置时间戳
            } else {
                statement.setNull(2, Types.TIMESTAMP); // 否则设为NULL
            }
            statement.setLong(3, accountId); // 设置账号ID
            statement.setLong(4, expectedVersion); // 设置预期版本号
            return statement.executeUpdate() == 1; // 执行并返回是否更新成功（1是代表成功）
        } catch (SQLException exception) {
            throw new AccountSecurityPersistenceException("failed to update account security state", exception);
        }
    }
}
