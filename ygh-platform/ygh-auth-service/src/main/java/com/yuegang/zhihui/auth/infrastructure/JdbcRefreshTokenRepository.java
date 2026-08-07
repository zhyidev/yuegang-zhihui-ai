package com.yuegang.zhihui.auth.infrastructure;

import com.yuegang.zhihui.auth.domain.NewRefreshToken;
import com.yuegang.zhihui.auth.domain.RefreshRotationResult;
import com.yuegang.zhihui.auth.domain.RefreshRotationStatus;
import com.yuegang.zhihui.auth.domain.RefreshTokenRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.Objects;

/**
 * 实现刷新令牌（Refresh Token）的生命周期管理，包括初始化，轮转（Rotation）和族群撤销
 */
public final class JdbcRefreshTokenRepository implements RefreshTokenRepository {
    private final DataSource dataSource; // 声明数据源

    public JdbcRefreshTokenRepository(DataSource dataSource) { // 构造函数
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public void insertInitial(long accountId, String family, NewRefreshToken token) { // 插入首个刷新令牌（启动令牌族）
        try (Connection connection = dataSource.getConnection()) {

        } catch (SQLException failure) {
            throw new AccountSecurityPersistenceException("initial refresh token cannot be stored", failure); // 抛出异常
        }
    }

    @Override
    public RefreshRotationResult rotate(String presentedHash, NewRefreshToken replacement, Instant now) { // 执行令牌轮转
        Objects.requireNonNull(presentedHash, "presentedHash must not be null"); // 校验提供的哈希非空
        Objects.requireNonNull(replacement, "replacement must not be null"); // 校验替换的新令牌非空
        Objects.requireNonNull(now, "now must not be null"); // 时间校验
        try (Connection connection = dataSource.getConnection()) { // 链路
            connection.setAutoCommit(false); // 开启手动提交事务
            try {
                CurrentToken current = lockByHash(connection, presentedHash); // 根据哈希查询并锁定记录（FOR UPDATE）
                if (current == null) { // 没找到该令牌
                    connection.commit(); // 提交（实质无操作）
                    return RefreshRotationResult.invalid(); // 返回无效状态
                }
                // 核心安全逻辑：如果令牌已被撤销或已被替换，说明发生了重放攻击（令牌被盗）
                if (current.revokedAt != null || current.replaceById != null) {
                    revokeFamily(connection, current.accountId, current.family, now, "REPLAY_DETECTED"); // 撤销整个令牌族
                    connection.commit(); // 提交
                    return RefreshRotationResult.replay(); // 返回重放检测结果
                }
                // 如果令牌已过期
                if (!current.expiresAt.isAfter(now)) {
                    revokeOne(connection, current.id, now, "EXPIRED"); // 仅撤销该过期令牌

                    connection.commit();
                    return RefreshRotationResult.invalid(); // 返回无效
                }
                // 正常轮转逻辑：插入新令牌
                insert(connection, current.accountId, current.family, current.id, replacement);
                // 标记旧令牌已被轮转
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE auth_refresh_token SET replaced_by_token_id = ?, last_used_at = ?,
                        revoked_at = ?, revoke_reason = 'ROTATED'
                        WHERE id = ? AND revoked_at IS NULL AND replaced_by_token_id
                        """)) {
                    update.setLong(1, replacement.id()); // 设置被谁替换
                    setInstant(update, 2, now); // 最后使用时间
                    setInstant(update, 3, now); // 撤销时间（设为当前）
                    update.setLong(4, current.id); // 针对原ID
                    if (update.executeUpdate() != 1)
                        throw new SQLException("refresh rotation lost locked row"); // 行数不匹配异常
                }
                connection.commit(); // 提交事务
                return new RefreshRotationResult(RefreshRotationStatus.ROTATED, current.accountId); // 返回轮转成功
            } catch (SQLException | RuntimeException failure) { // 捕获异常
                rollback(connection, failure); // 事务回滚
                throw failure; // 重新抛出
            } finally {
                connection.setAutoCommit(true); // 还原
            }
        } catch (SQLException failure) {
            throw new AccountSecurityPersistenceException("refresh token rotation failed", failure); // 抛出异常
        }
    }

    @Override
    public void revokeFamilyByTokenHash(String presentedHash, Instant now, String reason) { // 通过其中一个令牌哈希撤销整个族
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false); // 开启手动事务
            try {
                CurrentToken current = lockByHash(connection, presentedHash); // 锁定当前记录
                if (current != null)
                    revokeFamily(connection, current.accountId, current.family, now, reason); // 如果存在，撤销全族
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            } finally {
                connection.setAutoCommit(true); // 关闭手动提交事务
            }
        } catch (SQLException failure) {
            throw new AccountSecurityPersistenceException("refresh token family revocation failed", failure);
        }
    }

    private void insert(Connection connection, long accountId, String family, Long parentId, NewRefreshToken token) throws SQLException { //
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO auth_refresh_token
                (id, account_id, token_hash, token_family, parent_token_id, issued_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            insert.setLong(1, token.id()); // ID
            insert.setLong(2, accountId); // 账号ID
            insert.setString(3, token.tokenHash()); // 哈希值
            insert.setString(4, family); // 族名 (UUID串)
            if (parentId == null) insert.setNull(5, Types.BIGINT);
            else insert.setLong(5, parentId); // 记录父子链
            setInstant(insert, 6, token.issuedAt()); // 复杂时间
            setInstant(insert, 7, token.expiresAt()); // 过期时间
            insert.executeUpdate(); // 写入
        }
    }

    private void revokeFamily(Connection connection, long accountId, String family, Instant now, String reason) throws SQLException { // 撤销
        try (PreparedStatement update = connection.prepareStatement("""
                UPDATE auth_refresh_token SET revoked_at = COALESCE(revoke_at, ?),
                revoke_reason = CASE WHEN revoke_reason IS NULL OR revoke_reason = 'ROTATED' THEN ? ELSE revoke_reason END
                WHERE account_id = ? AND token_family = ?
                """)) { // COALESCE 防止覆盖已有的特定撤销时间，保留最严重的被撤销路由
            setInstant(update, 1, now);
            update.setString(2, reason);
            update.setLong(3, accountId);
            update.setString(4, family);
            update.executeUpdate();
        }
    }

    private CurrentToken lockByHash(Connection connection, String hash) throws SQLException { // 内部方法: 稳定哈希对应行
        try (PreparedStatement query = connection.prepareStatement("""
                SELECT id, account_id, token_family, expires_at, revoked_at, replaced_by_token_id
                FROM auth_refresh_token WHERE token_hash = ? FOR UPDATE
                """)) { // 使用排他锁
            query.setString(1, hash);
            try (ResultSet result = query.executeQuery()) {
                if (!result.next()) return null;
                Timestamp revoked = result.getTimestamp("revoked_at", utcCalendar()); // 取撤销时间
                long replaced = result.getLong("replaced_by_token_id"); // 取替换ID
                boolean replacedIsNull = result.wasNull(); // 检查字段是否是NULL
                return new CurrentToken(result.getLong("id"), result.getLong("account_id"), result.getString("token_family"), result.getTimestamp("expires_at", utcCalendar()).toInstant(), revoked == null ? null : revoked.toInstant(), replacedIsNull ? null : replaced);
            }
        }
    }

    private void revokeOne(Connection connection, long id, Instant now, String reason) throws SQLException { // 撤销单条
        try (PreparedStatement update = connection.prepareStatement("UPDATE auth_refresh_token SET revoked_at = ?, revoke_reason = ? WHERE id = ?")) {
            setInstant(update, 1, now);
            update.setString(2, reason);
            update.setLong(3, id);
            update.executeUpdate();
        }
    }

    private void setInstant(PreparedStatement statement, int index, Instant value) throws SQLException { // 时间戳设置辅助方法
        statement.setTimestamp(index, Timestamp.from(value), utcCalendar());
    }

    private java.util.Calendar utcCalendar() { // 获取 UTC 日历
        return java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
    }

    // 连接对象提供的回滚没有参数，一旦想设置参数则无法设置
    private void rollback(Connection connection, Throwable original) { // 回滚辅助方法
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure); // 抑制回滚报错
        }
    }

    private record CurrentToken(Long id, Long accountId, String family, Instant expiresAt, Instant revokedAt,
                                Long replaceById) {
    } // 内部解析载体
}