package com.yuegang.zhihui.user.infrastructure;

import com.yuegang.zhihui.user.api.AddressView;
import com.yuegang.zhihui.user.api.CreateAddressRequest;
import com.yuegang.zhihui.user.api.UpdateAddressRequest;
import com.yuegang.zhihui.user.domain.AddressRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public class JdbcAddressRepository implements AddressRepository { //实现地址仓储接口

    private static final String SELECT = """
            SELECT id, user_id, label, recipient_name_ciphertext, recipient_phone_ciphertext, pii_key_version, country_code, province_code, province_name, city_name, district_name,address_detail_ciphertext , postal_code, is_default, version, updated_at FROM user_address
            """; // 定义基础的查询sQL语句

    private final JdbcTemplate jdbc; // 定义核心组件
    private final TransactionTemplate tx; // 定义核心组件
    private final AddressCipher cipher; // 定义核心组件

    public JdbcAddressRepository(DataSource dataSource, AddressCipher cipher) {
        this.jdbc = new JdbcTemplate(dataSource); // 初始化 JDBC 模板
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource)); // 初始化事务模板
        this.cipher = Objects.requireNonNull(cipher); // 校验注入加密器
    }

    @Override
    public List<AddressView> findAll(long userId) { // 查找用户的所有地址
        // 按默认状态置顶，更新时间倒序，ID倒序排序
        return jdbc.query(SELECT + " WHERE user_id=? ORDER BY is_default DESC,updated_at DESC,id DESC", this::map, userId);
    }

    @Override
    public AddressView create(long id, long userId, CreateAddressRequest r) { // 创建新地址
        return Objects.requireNonNull(tx.execute(status -> { // 开启事务
            boolean makeDefault = r.defaultAddress() || count(userId) == 0; // 如果用户指定或这是一条地址，则设为默认
            if (makeDefault) clearDefault(userId); // 如果新地址是默认的，先清除旧默认标记
            jdbc.update("""
                            INSERT INTO user_address(id,user_id,label,recipient_name_ciphertext,recipient_phone_ciphertext,pii_key_version,
                                                     country_code,province_code,province_name,city_name,district_name,address_detail_ciphertext,postal_code,is_default)
                            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                            """, id, userId, blankToNull(r.label()), cipher.encrypt(userId, "recipient_name", r.recipientName()), // 加密姓名
                    cipher.encrypt(userId, "recipient_phone", r.recipientPhone()), cipher.keyVersion(), r.countryCode(), blankToNull(r.provinceCode()), r.provinceName(), r.cityName(), r.districtName(), cipher.encrypt(userId, "addressDetail", r.addressDetail()), blankToNull(r.postalCode()) //加密详细地址
            );
            return one(id, userId).orElseThrow(); // 返回新创建的地址对象
        }));
    }

    @Override
    public Optional<AddressView> update(long id, long userId, UpdateAddressRequest r) { // 更新地址
        return tx.execute(status -> { // 开启事务
            Boolean currentlyDefault = lockedDefault(id, userId, r.version()); // 锁定当前行并获取默认状态
            if (currentlyDefault == null) return Optional.empty(); // 记录不存在返回空
            if (r.defaultAddress() && !currentlyDefault) clearDefault(userId); // 若想改为默认且原本不是，则清除旧默认
            Boolean desireDefault = r.defaultAddress() || (currentlyDefault && count(userId) == 1); // 维持唯一的默认状态
            int changed = jdbc.update("""
                            UPDATE user_address SET
                            label=?,recipient_name_ciphertext=?,recipient_phone_ciphertext=?,
                            pii_key_version=?,country_code=?,province_code=?,province_name=?,city_name=?,district_name=?,
                            address_detail_ciphertext=?,postal_code=?,is_default=?,version=version+1
                            WHERE id=? AND user_id=? AND version=?
                            """, blankToNull(r.label()), cipher.encrypt(userId, "recipientName", r.recipientName()), // 更新重新加密
                    cipher.encrypt(userId, "recipientPhone", r.recipientPhone()), cipher.keyVersion(), r.countryCode(), blankToNull(r.provinceCode()), r.provinceName(), r.cityName(), r.districtName(), cipher.encrypt(userId, "addressDetail", r.addressDetail()), blankToNull(r.postalCode()), desireDefault, id, userId, r.version()); // 执行基于乐观锁 version 的更新
            if (changed == 1 && currentlyDefault && !desireDefault)
                promoteDefault(userId, id); // 若取消了默认且还有其他地址，自动选一个设为默认
            return changed == 1 ? one(id, userId) : Optional.empty(); // 返回登录
        });
    }

    @Override
    public boolean delete(long id, long userId, long version) { // 删除地址
        return Boolean.TRUE.equals(tx.execute(status -> { // 开启事务
            Boolean wasDefault = jdbc.query("SELECT is_default FROM user_address WHERE id=? AND user_id=? AND version=? FOR UPDATE",
                    rs -> rs.next() ? rs.getBoolean(1) : null, id, userId, version); // 查询并锁定即将删除的行
            if (wasDefault == null || jdbc.update("DELETE FROM user_address WHERE id=? AND user_id=? AND version=?", id, userId, version) != 1)
                return false; // 若不存在或删除失败，返回 false
            if (wasDefault) promoteDefault(userId, id); // 如果删除的是默认地址，自动顺位提升一个新地址为默认
            return true;
        }));
    }

    @Override
    public Optional<AddressView> makeDefault(long id, long userId, long version) { // 手动设为默认
        return tx.execute(status -> {
            Boolean currentlyDefault = lockedDefault(id, userId, version); // 锁定检查
            if (currentlyDefault == null) return Optional.empty(); // 不存在
            if (!currentlyDefault) clearDefault(userId); // 若当前非默认，先清空其他默认项
            if (jdbc.update("UPDATE user_address SET is_default=TRUE,version=version+1 WHERE id=? AND user_id=? AND version=?",
                    id, userId, version) != 1)
                return Optional.empty(); // 更新失败（乐观锁冲突）
            return one(id, userId); // 返回结果
        });
    }


    private void clearDefault(long userId) {
        jdbc.update("UPDATE user_address SET is_default=FALSE, version=version+1 WHERE user_id=? AND is_default=TRUE", userId);
    } //内部方法:清除用户所有默认标记

    private void promoteDefault(long userId, long excludeId) { // 内部方法：选举最新的地址作为默认地址
        jdbc.update("""
                UPDATE user_address SET is_default=TRUE,version=version+1 WHERE id=(
                    SELECT id FROM (SELECT id FROM user_address WHERE user_id=? AND id<>? ORDER BY updated_at DESC,id DESC LIMIT 1)
                    candidate)
                """, userId, excludeId);
    }

    private int count(long userId) { // 内部方法：统计用户地址总数
        return Optional.ofNullable(jdbc.queryForObject("SELECT COUNT(*) FROM user_address WHERE user_id=?", Integer.class, userId)).orElse(0);
    }

    private Boolean lockedDefault(long id, long userId, long version) { // 内部方法: 悲观锁定行
        return jdbc.query("SELECT is_default FROM user_address WHERE id=? AND user_id=? AND version=? FOR UPDATE", rs -> rs.next() ? rs.getBoolean(1) : null, id, userId, version);
    }

    private Optional<AddressView> one(long id, long userId) { // 内部方法：获取单条记录
        return jdbc.query(SELECT + " WHERE id=? AND user_id=?", rs -> rs.next() ? Optional.of(map(rs, 1)) : Optional.empty(), id, userId);
    }

    private AddressView map(ResultSet rs, int row) throws SQLException { // 将数据库结果继续映射为视图模型
        long owner = rs.getLong("user_id");
        int keyVersion = rs.getInt("pii_key_version");
        return new AddressView(Long.toString(rs.getLong("id")), rs.getString("label"), cipher.decrypt(owner, "recipientName", keyVersion, rs.getBytes("recipient_name_ciphertext")), // 解密姓名
                cipher.decrypt(owner, "recipientPhone", keyVersion, rs.getBytes("recipient_phone_ciphertext")), // 解密电话
                rs.getString("country_code"), rs.getString("province_code"), rs.getString("province_name"), rs.getString("city_name"), rs.getString("district_name"), cipher.decrypt(owner, "addressDetail", keyVersion, rs.getBytes("address_detail_ciphertext")), // 解密详细地址
                rs.getString("postal_code"), rs.getBoolean("is_default"), rs.getLong("version"), rs.getTimestamp("updated_at").toLocalDateTime().atOffset(ZoneOffset.UTC) // 按时间戳为 UTC OffsetDateTime
        );
    }


    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    } // 内部工具: 空字符串转 null
}