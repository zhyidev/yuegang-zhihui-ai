package com.yuegang.zhihui.order.application;

import com.yuegang.zhihui.inventory.api.InventoryCommand;
import com.yuegang.zhihui.order.infrastructure.InventoryClient;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public final class RefundInventoryCoordinator {
    private final OrderService orders;
    private final InventoryClient inventory;
    private final JdbcTemplate jdbc;

    public RefundInventoryCoordinator(OrderService o, InventoryClient i, DataSource d) {
        orders = o;
        inventory = i;
        jdbc = new JdbcTemplate(d);
    }

    private static InventoryCommand command(String request, String sku, long quantity) {
        String key = request + ":" + sku;
        if (key.length() > 128) key = hex(key);
        return new InventoryCommand(key, sku, quantity, "ORDER", request);
    }

    private static String hex(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void refundSucceeded(String event, String order) {
        orders.refundSucceeded(event, order);
        for (var c : commands(order)) inventory.returnSold(c);
    }

    private List<InventoryCommand> commands(String order) {
        return jdbc.query("SELECT m.request_id,i.sku_id,i.quantity FROM order_main m JOIN order_item i ON i.order_id=m.id WHERE m.id=?", (r, n) -> command(r.getString(1), Long.toString(r.getLong(2)), r.getLong(3)), Long.parseLong(order));
    }
}
