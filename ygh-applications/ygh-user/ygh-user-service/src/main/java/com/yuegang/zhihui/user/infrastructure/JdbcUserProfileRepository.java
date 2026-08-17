package com.yuegang.zhihui.user.infrastructure;

import com.yuegang.zhihui.user.api.UpdateUserProfileRequest;
import com.yuegang.zhihui.user.api.UserProfileView;
import com.yuegang.zhihui.user.domain.UserProfileRepository;
import java.sql.*;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

public final class JdbcUserProfileRepository implements UserProfileRepository {

    private final DataSource dataSource; // 声明原始数据源
    private final AddressCipher cipher; // 声明加密器

    public JdbcUserProfileRepository(DataSource dataSource, AddressCipher cipher) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.cipher = Objects.requireNonNull(cipher);
    }

    private static Long lockVersion(Connection c, long userId)
            throws SQLException { // 内部方法：通过 FOR UPDATE 锁定记录
        try (var s =
                c.prepareStatement("SELECT version FROM user_profile WHERE user_id=? FOR UPDATE")) {
            s.setLong(1, userId);
            try (var rows = s.executeQuery()) {
                return rows.next() ? rows.getLong(1) : null;
            }
        }
    }

    private static String normalizedEmail(String value) { // 内部工具：把邮箱标准化（转小写）
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) { // 内部工具: 空转 NULL
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    public Optional<UserProfileView> findByUserId(long userId) { // 按 ID 查询资料
        try (var c = dataSource.getConnection();
                var s =
                        c.prepareStatement(
                                "SELECT user_id,display_name,avatar_url,phone_ciphertext,email_ciphertext, contact_pii_key_version,"
                                        + "locale,timezone,version FROM user_profile WHERE user_id=?")) {
            s.setLong(1, userId); // 设置查询参数
            try (var rows = s.executeQuery()) {
                return read(rows); // 执行并读取结果
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("user profile cannot be read", failure);
        }
    }

    @Override
    public Optional<UserProfileView> save(
            long userId, UpdateUserProfileRequest request) { // 保存（新增或更新）
        try (var c = dataSource.getConnection()) { // 手动获取连接
            c.setAutoCommit(false); // 关键：关闭自动提交，开启手动事务
            try {
                Long current = lockVersion(c, userId); // 尝试加锁获取当期那版本号
                if (current == null) { // 场景：数据库中尚无此用户资料
                    if (request.version() != 0) {
                        c.rollback();
                        return Optional.empty();
                    } // 校验：新记录版本必须为 0
                    try (var s =
                            c.prepareStatement(
                                    """
                        INSERT IGNORE INTO
                        user_profile(user_id,display_name,avatar_url,phone_ciphertext,email_ciphertext,
                        contact_pii_key_version,locale,timezone,profile_completed,version) VALUES(?,?,?,?,?,?,?,?,?,TRUE, 0)
                        """)) {
                        bindInsert(s, userId, request);
                        s.executeUpdate(); // 执行数据插入操作
                    }
                } else { // 场景：更新现有资料
                    if (current != request.version()) {
                        c.rollback();
                        return Optional.empty();
                    } // 乐观锁版本校验
                    try (var s =
                            c.prepareStatement(
                                    """
                        UPDATE user_profile SET
                        display_name=?,avatar_url=?,phone_ciphertext=?,email_ciphertext=?,
                        contact_pii_key_version=?,locale=?,timezone=?,profile_completed=TRUE,version=version+1
                        WHERE user_id=? AND version=?
                        """)) { // 执行更新
                        s.setString(1, request.displayName().trim());
                        s.setString(2, blankToNull(request.avatarUrl()));
                        setEncrypted(s, 3, userId, "profilePhone", request.phone()); // 加密存储手机
                        setEncrypted(
                                s,
                                4,
                                userId,
                                "profileEmail",
                                normalizedEmail(request.email())); // 加密存储邮箱
                        setKeyVersion(s, 5, request.phone(), request.email()); // 设置密钥版本标记
                        s.setString(6, request.locale());
                        s.setString(7, request.timezone());
                        s.setLong(8, userId);
                        s.setLong(9, current); // 设置 WHERE 条件参数
                        if (s.executeUpdate() != 1) {
                            c.rollback();
                            return Optional.empty();
                        } // 校验行变动
                    }
                    c.commit(); // 逻辑无误，提交事务
                }

            } catch (SQLException failure) {
                c.rollback();
                throw failure;
            } // 发生 SQL 异常执行回滚
        } catch (SQLException failure) {
            throw new IllegalStateException("user profile cannot be saved", failure);
        }
        return findByUserId(userId); // 返回保存后的最新视图
    }

    private void bindInsert(PreparedStatement s, long userId, UpdateUserProfileRequest r)
            throws SQLException {
        s.setLong(1, userId);
        s.setString(2, r.displayName().trim());
        s.setString(3, blankToNull(r.avatarUrl()));
        setEncrypted(s, 4, userId, "profilePhone", r.phone());
        setEncrypted(s, 5, userId, "profileEmail", normalizedEmail(r.email()));
        setKeyVersion(s, 6, r.phone(), r.email());
        s.setString(7, r.locale());
        s.setString(8, r.timezone());
    }

    private Optional<UserProfileView> read(ResultSet rows) throws SQLException { // 内部方法：从结果集读取并解密
        if (!rows.next()) return Optional.empty();
        long userId = rows.getLong("user_id");
        byte[] phone = rows.getBytes("phone_ciphertext");
        byte[] email = rows.getBytes("email_ciphertext");
        int keyVersion = rows.getInt("contact_pii_key_version");
        return Optional.of(
                new UserProfileView(
                        Long.toString(userId),
                        rows.getString("display_name"),
                        rows.getString("avatar_url"),
                        decrypt(userId, "profilePhone", keyVersion, phone), // 解密手机
                        decrypt(userId, "profileEmail", keyVersion, email),
                        rows.getString("locale"), // 解密邮箱
                        rows.getString("timezone"),
                        rows.getLong("version")));
    }

    private void setEncrypted(
            PreparedStatement statement, int index, long userId, String field, String value)
            throws SQLException { // 内部方法:执行加密锁定
        String normalized = blankToNull(value);
        if (normalized == null) {
            statement.setNull(index, Types.VARBINARY);
            return; // 为空则存数据库 NULL
        }
        statement.setBytes(index, cipher.encrypt(userId, field, normalized)); // 执行加密
    }

    private void setKeyVersion(PreparedStatement statement, int index, String phone, String email)
            throws SQLException { // 内部方法: 处理密钥
        if (blankToNull(phone) == null && blankToNull(email) == null)
            statement.setNull(index, Types.SMALLINT);
        else statement.setInt(index, cipher.keyVersion()); // 只有存在加密字段时才记录密钥版本
    }

    private String decrypt(long userId, String field, int keyVersion, byte[] value) { // 内部解密代理方法
        return value == null ? null : cipher.decrypt(userId, field, keyVersion, value);
    }
}
