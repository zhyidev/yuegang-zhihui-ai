package com.yuegang.zhihui.auth.infrastructure;

import com.yuegang.zhihui.auth.domain.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class JdbcLoginAccountRepository implements LoginAccountRepository {
    private static final String FIND_SQL = """
            SELECT a.id, a.user_id, a.account_type, a.status,
            c.password_hash, c.password_algorithm, c.password_version
            FROM auth_account a
            JOIN auth_credential c ON c.account_id = a.id
            WHERE a.principal = ?
            """; // 关键查询账号与凭据的SQL（通过标准化凭据）

    private static final String FIND_BY_ID_SQL = """
            SELECT a.id, a.user_id, a.account_type, a.status,
            c.password_hash, c.password_algorithm, c.password_version
            FROM auth_account a JOIN auth_credential c ON c.account_id = a.id
            WHERE a.id = ?
            """; // 通过账号ID查询的SQL

    private final DataSource dataSource; // 声明数据源

    public JdbcLoginAccountRepository(DataSource dataSource) { // 注入构造函数
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    private static Optional<LoginAccount> read(java.sql.ResultSet rows) throws SQLException { // 读取结果集的私有静态方法
        if (!rows.next()) return Optional.empty(); // 没数据返回空
        return Optional.of(new LoginAccount( // 映射到 LoginAccount 对象
                rows.getLong("id"), rows.getLong("user_id"),
                rows.getString("account_type"),
                AccountStatus.valueOf(rows.getString("status")), // 转换状态枚举
                new PasswordDigest(rows.getString("password_hash"), // 转换密码摘要
                        rows.getString("password_algorithm"),
                        rows.getInt("password_version"))));
    }

    @Override
    public Optional<LoginAccount> findByPrincipal(String normalizedPrincipal) { // 通过凭据查询
        String principal = PrincipalNormalizer.normalize(normalizedPrincipal); // 再次规范化
        try (var connection = dataSource.getConnection(); // 链接
             var statement = connection.prepareStatement(FIND_SQL)) { // 预处理
            try (var rows = statement.executeQuery()) {
                return read(rows);
            } // 读取结果
        } catch (SQLException | IllegalArgumentException failure) { // 异常捕获
            throw new AccountSecurityPersistenceException("login account cannot be read", failure);
        }
    }

    @Override
    public Optional<LoginAccount> findByAccountId(long accountId) { // 通过ID查询
        if (accountId == 0) throw new IllegalArgumentException("accountId must be positive");
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, accountId); // 设置参数
            try (var rows = statement.executeQuery()) {
                return read(rows);
            } // 读取
        } catch (SQLException | IllegalArgumentException failure) { // 异常捕获
            throw new AccountSecurityPersistenceException("login account cannot be read", failure);
        }
    }

    @Override
    public LoginAccount create(long accountId, long userId, String normalizedPrincipal,
                               String accountType, PasswordDigest passwordDigest) { // 创建新账号
        String principal = PrincipalNormalizer.normalize(normalizedPrincipal); // 规范化
        var account = new LoginAccount(accountId, userId, accountType, AccountStatus.ACTIVE, passwordDigest); // 准备领域对象
        try (var connection = dataSource.getConnection()) { // 准备链接
            connection.setAutoCommit(false); // 启动手动事务
            try (var accountStatement = connection.prepareStatement("""
                    INSERT INTO auth_account
                    (id,user_id,principal,account_type,status,failed_login_count,version)
                    VALUES(?,?,?,?,'ACTIVE',0,0)
                    """); // 插入账号主表
                 var credentialStatement = connection.prepareStatement("""
                         INSERT INTO auth_credential
                         (id,account_id,password_hash,password_algorithm,password_version,changed_at)
                         VALUES(?,?,?,?,?,CURRENT_TIMESTAMP(6))
                         """)) { // 插入凭据表

                accountStatement.setLong(1, accountId); // 设置主表参数
                accountStatement.setLong(2, userId);
                accountStatement.setString(3, principal);
                accountStatement.setString(4, accountType);
                accountStatement.executeUpdate(); // 执行主表插入
                credentialStatement.setLong(1, accountId); // 设置凭据表参数
                credentialStatement.setLong(2, accountId);
                credentialStatement.setString(3, passwordDigest.hash());
                credentialStatement.setString(4, passwordDigest.algorithm());
                credentialStatement.setInt(5, passwordDigest.version());
                credentialStatement.executeUpdate(); // 执行凭据插入
                connection.commit(); // 提交事务
                return account; // 返回创建的账号
            } catch (SQLException failure) {
                connection.rollback(); // 报错回滚
                throw failure;
            } finally {
                connection.setAutoCommit(true); // 还原连接状态
            }
        } catch (SQLException failure) {
            if ("23000".equals(failure.getSQLState()))
                throw new AccountAlreadyExistsException(failure); // 如果是主键/唯一索引冲突，抛出特定异常
            throw new AccountSecurityPersistenceException("login account cannot be created", failure);
        }
    }
}
