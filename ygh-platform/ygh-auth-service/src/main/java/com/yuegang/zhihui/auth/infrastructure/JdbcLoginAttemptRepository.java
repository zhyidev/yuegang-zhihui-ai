package com.yuegang.zhihui.auth.infrastructure;

import com.yuegang.zhihui.auth.domain.LoginAttempt;
import com.yuegang.zhihui.auth.domain.LoginAttemptRepository;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.LongSupplier;

/**
 * 存储登录尝试的审计记录，使用安全随机数生成ID
 */
public final class JdbcLoginAttemptRepository implements LoginAttemptRepository {
    private static final String INSERT_SQL = """
            INSERT INTO auth_login_attempt
            (id, account_id, principal_hash, client_ip_hash, result, failure_reason, occurred_at, trace_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """; // 审计日志插入SQL

    private final DataSource dataSource; // 声明数据源
    private final LongSupplier idSupplier; // 声明ID供应器


    public JdbcLoginAttemptRepository(DataSource dataSource) { // 默认构造函数：使用正随机数生成ID
        this(dataSource, positiveRandomIds());
    }

    public JdbcLoginAttemptRepository(DataSource dataSource, LongSupplier idSupplier) { // 注入构造函数
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.idSupplier = Objects.requireNonNull(idSupplier, "idSupplier must not be null");
    }

    private static LongSupplier positiveRandomIds() { // 内部方法：返回一个生成正随机长整型的供应商
        SecureRandom random = new SecureRandom(); // 实例化安全随机数
        return () -> random.nextLong(1, Long.MAX_VALUE); // 返回 1 到 Max 之间值
    }

    private static Calendar utc() { // 返回 UTC 时区的日历实例
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    /**
     * 保存登录尝试的审计记录
     *
     * @param attempt 登录尝试对象
     */
    @Override
    public void save(LoginAttempt attempt) { // 保持审计记录
        Objects.requireNonNull(attempt, "attempt must not be null"); // 非空检查
        long id = idSupplier.getAsLong(); // 获取新的随机ID
        if (id <= 0) throw new IllegalArgumentException("login audit supplier returned a non-positive value"); // ID必须为正
        try (var connection = dataSource.getConnection(); // 链接
             var statement = connection.prepareStatement(INSERT_SQL)) { // 预处理
            statement.setLong(1, id); // 设置ID
            if (attempt.accountId() == null) statement.setNull(2, Types.BIGINT); // 账号ID可能为NULL
            else statement.setLong(2, attempt.accountId());
            statement.setString(3, attempt.principalHash()); // 设置凭据哈希
            statement.setString(4, attempt.clientIpHash()); // 设置IP哈希
            statement.setString(5, attempt.result().name()); // 设置结果名
            if (attempt.failureReason() == null) statement.setNull(6, Types.VARCHAR); // 失败原因可能为空
            else statement.setString(6, attempt.failureReason());
            statement.setTimestamp(7, Timestamp.from(attempt.occurredAt()), utc()); // 强制使用 UTC 时区保存时间戳
            statement.setString(8, attempt.traceId()); // 保存追踪ID
            if (statement.executeUpdate() != 1) {
                throw new SQLException("login audit insert did not affect exactly one row");
            }
        } catch (SQLException failure) {
            throw new AccountSecurityPersistenceException("login audit cannot be stored", failure);
        }
    }
}
