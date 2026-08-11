package com.yuegang.zhihui.order.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.order.api.CartItemRequest;
import com.yuegang.zhihui.order.api.CartItemView;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

public final class CartService {
    private final JdbcTemplate jdbc;

    public CartService(DataSource d) {
        jdbc = new JdbcTemplate(d);
    }

    private static long next() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    private static long id(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    public List<CartItemView> list(long user) {
        return jdbc.query("SELECT id,sku_id,quantity,selected,version FROM order_cart_item WHERE user_id=? ORDER BY updated_at DESC", (r, n) -> new CartItemView(Long.toString(r.getLong(1)), Long.toString(r.getLong(2)), r.getLong(3), r.getBoolean(4), r.getLong(5)), user);
    }

    public CartItemView save(long user, CartItemRequest c) {
        long sku = id(c.skuId());
        var current = jdbc.query("SELECT id,version FROM order_cart_item WHERE user_id=? AND sku_id=?", r -> r.next() ? new long[]{r.getLong(1), r.getLong(2)} : null, user, sku);
        long item;
        if (current == null) {
            if (c.version() != 0) throw new BusinessException(ErrorCode.BUSINESS_CONFLICT);
            item = next();
            jdbc.update("INSERT INTO order_cart_item(id,user_id,sku_id,quantity,selected) VALUES(?,?,?,?,?)", item, user, sku, c.quantity(), c.selected());
        } else {
            item = current[0];
            if (jdbc.update("UPDATE order_cart_item SET quantity=?,selected=?,version=version+1 WHERE id=? AND user_id=? AND version=?", c.quantity(), c.selected(), item, user, c.version()) != 1)
                throw new BusinessException(ErrorCode.BUSINESS_CONFLICT);
        }
        return jdbc.query("SELECT id,sku_id,quantity,selected,version FROM order_cart_item WHERE id=? AND user_id=?", r -> {
            r.next();
            return new CartItemView(Long.toString(r.getLong(1)), Long.toString(r.getLong(2)), r.getLong(3), r.getBoolean(4), r.getLong(5));
        }, item, user);
    }

    public void delete(long user, String item, long version) {
        if (jdbc.update("DELETE FROM order_cart_item WHERE id=? AND user_id=? AND version=?", id(item), user, version) != 1)
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
