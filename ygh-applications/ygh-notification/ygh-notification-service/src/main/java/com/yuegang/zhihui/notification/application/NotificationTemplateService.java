package com.yuegang.zhihui.notification.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.notification.api.NotificationTemplateView;
import com.yuegang.zhihui.notification.api.SaveNotificationTemplateRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * 模板管理服务类，负责模板的查询和带版本控制（乐观锁）的保存
 */
@Service // 标识为 Spring 服务组件
public final class NotificationTemplateService { // 定义模板服务类
    private final JdbcTemplate jdbc; // 声明操作模板

    public NotificationTemplateService(DataSource dataSource) { // 构造函数初始化
        this.jdbc = new JdbcTemplate(dataSource);
    }

    private static NotificationTemplateView map(ResultSet row) throws SQLException { // 辅助方法：结果集映射至 Record
        return new NotificationTemplateView(row.getString(1), row.getString(2),
            row.getString(3), row.getString(4), row.getBoolean(5), row.getLong(6));
    }

    private static long nextId() { // 辅助方法: 生成唯一分布式 ID
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    public List<NotificationTemplateView> list() { // 查询所有的通知模板列表
        // 按照模板编码 code 进行排序返回视图列表
        return jdbc.query("SELECT code,title_template,context_template,channel,enabled,version FROM notification_template ORDER BY code",
            (row, n) -> map(row));
    }

    public NotificationTemplateView save(String code, SaveNotificationTemplateRequest request) { // 创建或更新通知模板
        if (!code.equals(request.code()))
            throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 路径参数与 Body 参数不匹配则报错
        // 尝试执行更新语句，包含版本号校验（乐观锁）
        int changed = jdbc.update("UPDATE notification_template SET title_template=?,content_template=?,channel=?,enabled=?,version=version+1 WHERE code=? AND version=?",
            request.titleTemplate(), request.contentTemplate(), request.channel(), request.enabled(), code, request.version());

        // 如果更新失败且版本号为 0，说明该 code 的模板尚不存在，尝试执行插入
        if (changed == 0 && request.version() == 0) {
            try {
                // 插入新模板数据
                jdbc.update("INSERT INTO notification_template(id,code,title_template,content_template,channel,enabled) VALUES(?,?,?,?,?,?)",
                    nextId(), code, request.titleTemplate(), request.contentTemplate(), request.channel(), request.enabled());
            } catch (DuplicateKeyException exception) { // 处理并发插入导致的主键重复
                throw new BusinessException(ErrorCode.BUSINESS_CONFLICT);
            }
        } else if (changed == 0) { // 更新操作失败通常是因为版本号不匹配（已被他人修改）
            throw new BusinessException(ErrorCode.BUSINESS_CONFLICT);
        }


        return find(code); // 返回最新保存的模板详情
    }

    private NotificationTemplateView find(String code) { // 内部方法：查找指定 code 的模板
        return jdbc.query("SELECT code,title_template,content_template,channel,enabled,version FROM notification_template WHERE code=?",
            result -> {
                if (!result.next()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND); // 没找到则抛出 404
                return map(result); // 映射为视图
            }, code);
    }

}
