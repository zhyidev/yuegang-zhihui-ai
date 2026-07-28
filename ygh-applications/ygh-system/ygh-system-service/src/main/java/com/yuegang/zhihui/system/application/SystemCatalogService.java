package com.yuegang.zhihui.system.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.system.api.DictionaryView;
import com.yuegang.zhihui.system.api.FeatureFlagView;
import com.yuegang.zhihui.system.api.UpdateFeatureFlagRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

// 该类负责系统字典查看与功能开关（Feature Flag）的运行时管理。
public class SystemCatalogService { // 定义最终类：系统目录服务
    private final JdbcTemplate jdbc; // 声明 JDBC 模板

    public SystemCatalogService(DataSource d) { // 构造函数
        this.jdbc = new JdbcTemplate(d); // 初始化模板
    } // 结束

    public List<DictionaryView> dictionaries() { // 方法：查询所有已启用的字典内容（业务端调用）
        return jdbc.query("SELECT code,name FROM system_dictionary_type WHERE enabled=TRUE ORDER BY code",
                (ResultSet t, int n) -> new DictionaryView(t.getString(1), t.getString(2),
                        jdbc.query("SELECT item_key,item_value,sort_order FROM system_dictionary_item i " +
                                        "JOIN system_dictionary_type t ON t.id=i.type_id WHERE t.code=?" +
                                        "AND i.enabled=TRUE ORDER BY i.sort_order,i.item_key",
                                (ResultSet i, int x) -> new DictionaryView.Item(i.getString(1), i.getString(2), i.getInt(3
                                )), t.getString(1)))); // 嵌套查询：查出字典类型后同步查出其下的所有启用明细项
    } // 结束

    public List<FeatureFlagView> flags() { // 方法：查询所有功能开关
        return jdbc.query("SELECT flag_key,enabled,rollout_percent,rules_json,version FROM system_feature_flag ORDER BY flag_key",
                (ResultSet r, int n) -> view(r)); // 查询并转换为视图列表
    }

    public FeatureFlagView updateFlag(String key, UpdateFeatureFlagRequest c, long operator) { // 方法：更新功能开关配置
        if (!key.matches("[a-z0-9._-]{1,127}"))
            throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 校验开关 Key 命名规范
        // 如果版本号为 0 执行插入，否则执行带版本校验的更新
        int n = c.version() == 0 ? jdbc.update("INSERT INTO system_feature_flag(flag_key,enabled,rollout_percent,rules_json,updated_by) " +
                        "VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                        "enabled=VALUES(enabled),rollout_percent=VALUES(rollout_percent)," +
                        "rules_json=VALUES(rules_json),updated_by=VALUES(updated_by), version=version+1", key, c.enabled(),
                c.rolloutPercent(), c.rulesJson(), operator) : jdbc.update("UPDATE system_feature_flag " +
                        "SET enabled=?,rollout_percent=?,rules_json=?,updated_by=?,version=version+1 WHERE flag_key=? AND version=?", c.enabled(),
                c.rolloutPercent(), c.rulesJson(), operator, key, c.version());
        if (n < 1) throw new BusinessException(ErrorCode.BUSINESS_CONFLICT); // 更新失败说明乐观锁冲突
        return jdbc.query("SELECT flag_key,enabled,rollout_percent,rules_json,version FROM system_feature_flag WHERE flag_key=?",
                r -> { // 查询并返回更新后的结果
                    if (!r.next()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
                    return view(r);
                }, key);

    } //  结束


    private static FeatureFlagView view(java.sql.ResultSet r) throws SQLException {
        return new FeatureFlagView(r.getString(1), r.getBoolean(2), r.getInt(3), r.getString(4), r.getLong(5)); // 从结果映射到 DTO
    }
}