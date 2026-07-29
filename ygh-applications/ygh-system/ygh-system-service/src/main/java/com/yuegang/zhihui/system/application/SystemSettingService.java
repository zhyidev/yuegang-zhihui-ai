package com.yuegang.zhihui.system.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.system.api.SystemSettingView;
import com.yuegang.zhihui.system.api.UpdateSystemSettingRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

// 该服务负责通用系统全局设置（Key-Value 格式）的管理，包含敏感数据脱敏与审计记录
public final class SystemSettingService { // 定义最终类，系统设置服务
    private final JdbcTemplate jdbc; // 声明模板

    public SystemSettingService(DataSource d) { // 构造函数
        jdbc = new JdbcTemplate(d); // 初始化
    }


    public List<SystemSettingView> list() { //方法:列出所有设置项
        return jdbc.query("SELECT setting_key, setting_value, value_type, secret, version FROM system_setting ORDER BY setting_key", (r, n) -> new SystemSettingView(r.getString("setting_key"), //r.getString("1"),
                r.getString("setting_value"), r.getString("value_type"), r.getBoolean("secret"), r.getLong("version"))); //逻辑：如果标记为secret（秘密），则对值进行脱敏显示（[REDACTED]）
    }

    public SystemSettingView update(String key, UpdateSystemSettingRequest c, long operator) { // 方法: 更新设置项
        if (!key.matches("[a-z][a-z0-9._-]{1,127}"))
            throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 校验 Key 命名规范
        String old = jdbc.query("SELECT setting_value FROM system_setting WHERE setting_key=?", r -> r.next() ? r.getString(1) : null, key); // 先获取修改前的旧值
        int n; // 声明影响行数
        if (old == null && c.version() == 0) { // 如果库中无值且请求版本为0，执行插入
            n = jdbc.update("INSERT INTO system_setting(setting_key,setting_value,value_type,secret,updated_by) VALUES(?,?,?,?,?)", key, c.value(), c.valueType(), c.secret(), operator);
        }
        else // 否则执行基于乐观锁的版本更新
            n = jdbc.update("UPDATE system_setting SET setting_value=?, value_type=?, secret=?, updated_by=?,version=version+1 WHERE setting_key=? AND version=?", c.value(), c.valueType(), c.secret(), operator, key, c.version());
        if( n!=1)
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT); // 并发修改冲突判定
//         记录配置审计日志：存入新旧值的 SHA-256 哈希值，而不存明文，以防审计表泄露
        jdbc.update("INSERT INTO system_configuration_audit(id,config_type,config_key,old_digest,new_digest,operator_user_id) VALUES(?,?,?,?,?,?)", next(), "SETTING", key,old == null ? null: digest(old), digest(c.value()), operator);
        return jdbc.query("SELECT setting_key,setting_value,value_type,secret,version FROM system_setting WHERE setting_key=?", r-> {
            r.next();
            return new SystemSettingView(r.getString(1),r.getBoolean(4) ? "[REDACTED]" : r.getString(2),r.getString(3),r.getBoolean(4),r.getLong(5));
        }, key);
    }

    private static String digest(String x) { // 私有静态方法: 生成哈希摘要
        try { // 计算 SHA-256
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(x.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    } // 结束

    private static long nextId() { // 内部 ID 生成
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    } // 结束
}