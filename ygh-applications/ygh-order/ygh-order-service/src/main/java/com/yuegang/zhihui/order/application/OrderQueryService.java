package com.yuegang.zhihui.order.application;

import com.yuegang.zhihui.order.api.OrderStatus;
import com.yuegang.zhihui.order.api.OrderView;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.List;

public final class OrderQueryService {
    private final JdbcTemplate jdbc;

    public OrderQueryService(DataSource d) {
        jdbc = new JdbcTemplate(d);
    }

    private static OrderView view(ResultSet r) throws SQLException {
        return new OrderView(Long.toString(r.getLong(1)), r.getString(2), Long.toString(r.getLong(3)), OrderStatus.valueOf(r.getString(4)), r.getBigDecimal(5), r.getString(6), r.getLong(7), r.getTimestamp(8).toLocalDateTime().atOffset(ZoneOffset.UTC));
    }

    private static int size(int n) {
        return Math.max(1, Math.min(n, 100));
    }

    public List<OrderView> mine(long user, String status, int limit) {
        String sql = "SELECT id,order_no,user_id,status,total_amount,currency,version,created_at FROM order_main WHERE user_id=?" + (status == null || status.isBlank() ? "" : " AND status=?") + " ORDER BY created_at DESC LIMIT ?";
        return status == null || status.isBlank() ? jdbc.query(sql, (r, n) -> view(r), user, size(limit)) : jdbc.query(sql, (r, n) -> view(r), user, status, size(limit));
    }

    public List<OrderView> admin(String status, int limit) {
        String sql = "SELECT id,order_no,user_id,status,total_amount,currency,version,created_at FROM order_main" + (status == null || status.isBlank() ? "" : " WHERE status=?") + " ORDER BY created_at DESC LIMIT ?";
        return status == null || status.isBlank() ? jdbc.query(sql, (r, n) -> view(r), size(limit)) : jdbc.query(sql, (r, n) -> view(r), status, size(limit));
    }
}
