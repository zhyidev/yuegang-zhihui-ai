package com.yuegang.zhihui.wallet.api;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.web.TraceIdResolver;
import com.yuegang.zhihui.wallet.security.WalletInternalVerifier;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.math.BigDecimal;

@RestController
@RequestMapping("/internal/v1/wallet/references")
public final class WalletInternalQueryController {
    private final JdbcTemplate jdbc;
    private final WalletInternalVerifier verifier;

    public WalletInternalQueryController(DataSource d, WalletInternalVerifier v) {
        jdbc = new JdbcTemplate(d);
        verifier = v;
    }

    @GetMapping("/{reference}")
    ApiResponse<WalletReferenceView> reference(@PathVariable String reference, HttpServletRequest r) {
        verifier.verify(r);
        WalletReferenceView view = jdbc.query("SELECT reference_id,user_id,MAX(type='PAYMENT' AND status='SUCCEEDED'),MAX(type='REFUND' AND status='SUCCEEDED'),COALESCE(SUM(CASE WHEN type='PAYMENT' AND status='SUCCEEDED' THEN amount ELSE 0 END),0),COALESCE(SUM(CASE WHEN type='REFUND' AND status='SUCCEEDED' THEN amount ELSE 0 END),0),MAX(currency) FROM wallet_transaction WHERE reference_id=? GROUP BY reference_id,user_id", x -> x.next() ? new WalletReferenceView(x.getString(1), Long.toString(x.getLong(2)), x.getBoolean(3), x.getBoolean(4), x.getBigDecimal(5), x.getBigDecimal(6), x.getString(7)) : new WalletReferenceView(reference, null, false, false, BigDecimal.ZERO, BigDecimal.ZERO, null), reference);
        return ApiResponse.success(view, TraceIdResolver.resolve(r));
    }
}
