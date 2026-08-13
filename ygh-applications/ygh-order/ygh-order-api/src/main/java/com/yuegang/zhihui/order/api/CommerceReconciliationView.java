package com.yuegang.zhihui.order.api;

import java.time.OffsetDateTime;
import java.util.List;

public record CommerceReconciliationView(long checkedOrders, long consistentOrders, List<Discrepancy> discrepancies,
                                         OffsetDateTime checkedAt) {
    public record Discrepancy(String orderId, String orderStatus, String dimension, String expected, String actual) {
    }
}
