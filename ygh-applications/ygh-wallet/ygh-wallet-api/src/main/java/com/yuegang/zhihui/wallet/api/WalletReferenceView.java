package com.yuegang.zhihui.wallet.api;

import java.math.BigDecimal;

public record WalletReferenceView(String referenceId, String userId, boolean paymentSucceeded, boolean refundSucceeded,
                                  BigDecimal paidAmount, BigDecimal refundedAmount, String currency) {
}
