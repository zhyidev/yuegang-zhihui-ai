package com.yuegang.zhihui.system.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.system.api.DictionaryAdminView;
import com.yuegang.zhihui.system.api.SaveDictionaryTypeRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import java.util.List;
import java.util.UUID;


// 该服务负责系统字典类型（分类）与字典项（值）的后台管理维护
public class SystemDictionaryAdministrationService { // 定义类:系统字典管理服务
    private final JdbcTemplate jdbc; // 声明 JDBC 模板

    public SystemDictionaryAdministrationService(DataSource d) {
        jdbc = new JdbcTemplate(d); //初始化
    }

    private static long next() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    } // 结束

    public List<DictionaryAdminView> all() { // 方法:查询全量字典配置(后台管理用)
        //查询并递归映射
        return jdbc.query("SELECT id,code,name,enabled,version FROM system_dictionary_type ORDER BY code", (r, n) -> view(r));
    }

    @Transactional //开启方法级事务
    public DictionaryAdminView saveType(SaveDictionaryTypeRequest c) { // 方法:保存字典类型
        //如果是新记录则执行插入，负责执行乐观锁更新
        int changed = c.version() == 0 ? jdbc.update("INSERT INTO system_dictionary_type(id,code,name,enabled) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),enabled=VALUES(enabled) , version=version+1", next(), c.code(), c.name(), c.enabled()) : jdbc.update("UPDATE system_dictionary_type SET name=?,enabled=?,version= version+1 WHERE code=? AND version=?", c.name(), c.enabled(), next(), c.code(), c.version());
        if (changed < 1) throw new BusinessException(ErrorCode.BUSINESS_CONFLICT); // 并发修改冲突报错
        return byCode(c.code()); // 返回最新状态
    } // end

    @Transactional
    public DictionaryAdminView saveItem(String code, SaveDictionaryItemRequest c) {
        Long type = jdbc.query("SELECT id FROM system_dictionary_type WHERE code=?", r -> r.next() ? r.getLong(1) : null, code); //先查找所属分类的ID
        if (type == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        // 实现保存字典项的逻辑
        int changed = c.version() == 0 ? jdbc.update("INSERT INTO system_dictionary_item(id,type_id,item_key,item_value,sort_order,enabled) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE item_value=VALUES(item_value),sort_order=VALUES(sort_order),enabled=VALUES(enabled),version=version+1", next(), type, c.key(), c.value(), c.sortOrder(), c.enabled()) : jdbc.update("UPDATE system_dictionary_item SET item_value=?,sort_order=?,enabled=?,version= version+1 WHERE type_id=? AND item_key=? AND version=?", c.value(), c.sortOrder(), c.enabled(), type, c.key(), c.version());
        if (changed < 1) throw new BusinessException(ErrorCode.BUSINESS_CONFLICT); // 并发修改冲突报错
        return byCode(code);
    }

    private DictionaryAdminView byCode(String code) { // 私有方法：根据编码查详情
        return jdbc.query("SELECT id,code,name,enabled,version FROM system_dictionary_type WHERE code=?", r -> { // 查询主表
            if (!r.next()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            return view(r); //
        }, code);
    }

    private DictionaryAdminView view(java.sql.ResultSet r) throws java.sql.SQLException { // 核心映射逻辑
        long id = r.getLong(1); // 获取主表 ID
        // 级联查询该类型下的所有明细项
        var items = jdbc.query("SELECT item_key,item_value,sort_order,enabled,version FROM system_dictionary_item WHERE type_id=? ORDER BY sort_order,item_key", (i, n) -> new DictionaryAdminView.Item(i.getString(1), i.getString(2), i.getInt(3), i.getBoolean(4), i.getLong(5)), id);
        return new DictionaryAdminView(r.getString(2), r.getString(3), r.getBoolean(4), r.getLong(5), items); // 组装返回
    } // 结果
}
