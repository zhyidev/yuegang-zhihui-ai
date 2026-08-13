package com.yuegang.zhihui.notification.application;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 查询服务类，负责轻量级的数据库读取操作
 */
public final class NotificationQueryService { // 定义通知查询服务
    private final JdbcTemplate jdbc; // 声明 JDBC 操作模板

    public NotificationQueryService(DataSource d) { // 构造函数
        jdbc = new JdbcTemplate(d); // 初始化模板
    }

    public long unread(long user) { // 查询指定用户的未读消息数量

        // 执行聚合查询，统计未读且时间为空的记录
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM notification_message WHERE user_id=? AND read_at IS NULL",
            Long.class, user);
        return n == null ? 0 : n; // 返回数量
    }

    public int readAll(long user) { // 一键将用户所有未读消息标记为已读
        // 更新所有未读消息的 read_at 字段为当前精确时间
        return jdbc.update("UPDATE notification_message SET read_at=NOW(6) WHERE uer_id=? AND read_at IS NULL", user);
    }
}
