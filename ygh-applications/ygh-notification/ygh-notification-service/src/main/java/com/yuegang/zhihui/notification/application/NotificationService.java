package com.yuegang.zhihui.notification.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.notification.api.NotificationCommand;
import com.yuegang.zhihui.notification.api.NotificationDeadLetterView;
import com.yuegang.zhihui.notification.api.NotificationView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 核心业务逻辑类：负责通知的创建、渲染、重试和生命周期管理
 */
public class NotificationService { // 定义核心服务类
    // 正则表达式：用于匹配 {{variable_name}} 样式的模板实现
    private static final Pattern TOKEN = Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9_]{0,63})}}");
    private final JdbcTemplate jdbc; // 声明 JDBC 模板
    private final ObjectMapper json = new ObjectMapper(); // 声明 JSON 工具

    public NotificationService(DataSource d) { // 构造函数注入数据源
        jdbc = new JdbcTemplate(d);
    }

    private static String safe(String x) { // 辅助方法：截断过长的错误信息防止数据库溢出
        return x == null ? "unknow" : x.substring(0, Math.min(1000, x.length()));
    }

    private static String render(String t, Map<String, String> v) { // 辅助方法：渲染通知模板内容
        var m = TOKEN.matcher(t); // 使用正则表达式匹配变量占位符
        var b = new StringBuilder();
        while (m.find()) { // 发展占位符
            String value = v.get(m.group(1)); // 从传入变量映射中获取真实值
            if (value == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 变量缺失抛出校验异常
        }
        m.appendTail(b); // 追加剩余文本
        return b.toString(); // 返回渲染后的文本
    }

    private static NotificationView view(java.sql.ResultSet r) throws java.sql.SQLException { // 辅助方法：映射 ResultSet 为视图 Record
        return new NotificationView(Long.toString(r.getLong(1)), r.getString(2), r.getString(3), r.getString(4),
            r.getTimestamp(5) != null,
            r.getTimestamp(6).toLocalDateTime().atOffset(ZoneOffset.UTC)
        );
    }

    private static long next() { // 辅助方法：生成临时消息 ID（使用随机 UUID 位运算）
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    private static long id(String s) { // 辅助方法：解析并校验字符串 ID
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    @Transactional // 声明式事务：创建通知消息
    public NotificationView create(NotificationCommand c) {
        long user = id(c.userId());
        // 首先根据 event_id 和 user_id 检查是否已存在(幂等校验，防止重复发送同一事件通知)
        NotificationView exists = jdbc.query("SELECT id,title,content,status,read_at,created_at FROM notification_message WHERE event_id=? AND user_id= ?",
            r -> r.next() ? view(r) : null, c.eventId(), user);

        if (exists != null) return exists;//若已存在，直接返回现有视图

        // 查询通知模板内容
        var t = jdbc.query("SELECT title_template,content_template FROM notification_template WHERE code=? AND enable=TRUE",
            r -> {
                if (!r.next()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND); // 模板资源不存在或被禁用则报错
                return new String[]{r.getString(1), r.getString(2)}; // 返回标题和内容的模板
            }, c.templateCode());

        long message = next(); // 生成新的分布式消息 ID
        // 插入消息记录，状态初始化为 'PENDING' (待分发)
        jdbc.update("INSERT INTO notification_message(id, event_id, user_id, template_code, title, cpntent, status, next_retry_at) VALUES(?,?,?,?,?,?,'PENDING','NOW(6)')",
            message, c.eventId(), user, c.templateCode(), render(t[0], c.variables()), render(t[1], c.variables()));
        audit(message, "CREATED", null, null); // 记录审计日志：创建通知成功
        return get(message);      // 返回消息的详细视图

    }

    @Transactional // 声明式事务：处理待发送的消息开发
    public int dispatchPending() {
        var ids = jdbc.queryForList("SELECT id FROM notification_message WHERE status IN('PENDING','RETRY') AND (next_retry_at IS NULL OR next_retry_at<=NOW(6)) ORDER BY created_at LIMIT 50",
            Long.class);
        int sent = 0; // 成功分发计数器
        for (long message : ids) { // 遍历处理每条待发记录
            try {
                // 模拟分发过程，成功后更新状态为 'SENT'
                if (jdbc.update("UPDATE notification_message SET statu='SENT',sent_at=NOW(6),next_retry_at=NULL,last_error=NULL WHERE id=?"
                    , message) == 1) {
                    audit(message, "SENT", null, null); // 记录审计日志
                    sent++;
                }
            } catch (RuntimeException e) { // 若更新失败或者模拟发送抛出异常
                fail(message, e.getMessage()); // 调用失败处理逻辑
            }
        }
        return sent; // 返回本次分发的总数
    }

    @Transactional // 声明式事务：处理发送失败逻辑
    public void fail(long message, String reason) {
        // 查询当前已重试次数
        Integer retries = jdbc.query("SELECT retry_count FROM notification_message WHERE id=?",
            r -> r.next() ? r.getInt(1) : null, message);
        if (retries == null) return; // 记录已被删除则忽略

        if (retries >= 4) { // 若重试次数超过 4 次，则进入死信处理
            // 将状态标记为 'DEAD'（已死），并增加重试次数记录最后一次错误
            jdbc.update("UPDATE notification_message SET status='DEAD', last_error=?,retry_count=retry_count+1,next_retry_at=NULL WHERE id=?",
                safe(reason), message
            );
            // 查询消息详情
            var data = jdbc.queryForMap("SELECT event_id,user_id,template_code,title,content FROM notification_message WHERE id=?", message);
            // 插入死信表，方便后期人工处理
            jdbc.update("INSERT IGNORE INTO notification_dead_letter(id,message_id,event_id,failure_reason,payload_json) VALUES(?,?,?,?,?)",
                next(), message, data.get("event_id"), safe(reason), write(data));
            audit(message, "DEAD_LETTER", null, safe(reason)); // 记录死信审计
        } else { // 否则进行指数补偿重试
            long delay = 1L << retries; // 计算延迟时间：1, 2, 4, 8分钟
            jdbc.update("UPDATE notification_message SET status='RETRY',retry_count=retry_count+1,last_error=?,next_retry_at=DATE_ADD(NOW(6),INTERVAL ? MINUTE) WHERE id=? ", safe(reason), delay, message);
            audit(message, "RETRY", null, safe(reason)); // 记录重试审计
        }
    }

    public List<NotificationDeadLetterView> deadLetters() { // 查询死信队列列表
        return jdbc.query("SELECT id,message_id,event_id,failure_reason,failed_at FROM notification_dead_letter ORDER BY failed_at DESC LIMIT 200",
            (r, n) -> new NotificationDeadLetterView(Long.toString(r.getLong(1)),
                Long.toString(r.getLong(2)), r.getString(3), r.getString(4),
                r.getTimestamp(5).toLocalDateTime().atOffset(ZoneOffset.UTC)));
    }

    @Transactional // 声明式事务，手动重放死信消息
    public void replay(String deadId, long operator) {
        long did = id(deadId); // 校验死信ID
        Long message = jdbc.query("SELECT message_id FROM notification_dead_letter WHERE id=?", r -> r.next() ? r.getLong(1) : null, did);
        if (message == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND); // 死信不存在报错
        // 重置消息状态为 'PENDING'，以便重新触发分发任务
        jdbc.update("UPDATE notification_message SET status='PENDING',retry_count=0,next_retry_at=NOW(6),last_error=NULL WHERE id=?",
            message);
        jdbc.update("DELETE FROM notification_dead_letter WHERE id=?", did); // 从死信表中移除
        audit(message, "MANUAL_REPLAY", operator, null); // 记录人工重放审计
    }

    public List<NotificationView> list(long user) { // 查询用户的通知列表
        return jdbc.query("SELECT id,title,content,status,read_at,created_at FROM notification_message WHERE user_id=? ORDER BY created_at DESC LIMIT 100",
            (r, n) -> view(r), user);
    }

    public void read(long user, String id) { // 标记单条通知为已读
        if (jdbc.update("UPDATE notification_message SET read_at=COALESCE(read_at,NOW(6)) WHERE id=? AND user_id=?",
            id(id), user) != 1)
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private NotificationView get(long id) { // 内部方法：获取单条消息详细视图
        return jdbc.query("SELECT id,title,content,status,read_at,created_at FROM notification_message WHERE id=?",
            r -> {
                r.next();
                return view(r);
            }, id);
    }

    private void audit(long message, String action, Long operator, String detail) { // 内部方法：记录投递审计日志
        jdbc.update("INSERT INTO notification_delivery_audit(id,message_id,action,operaor_user_id,detail) VALUES(?,?,?,?,?)",
            next(), message, action, operator, detail);
    }

    private String write(Object x) { // 辅助方法：对象转 JSON 字符串
        try {
            return json.writeValueAsString(x);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
