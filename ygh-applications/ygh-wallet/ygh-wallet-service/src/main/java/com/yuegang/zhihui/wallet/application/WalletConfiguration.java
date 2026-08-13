package com.yuegang.zhihui.wallet.application;

import com.yuegang.zhihui.wallet.security.WalletInternalVerifier;
import com.yuegang.zhihui.wallet.security.WalletUserResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;

@Configuration(proxyBeanMethods = false)
class WalletConfiguration {
    @Bean
    WalletService walletService(DataSource d) {
        return new WalletService(d);
    }

    @Bean
    WalletQueryService walletQueryService(DataSource d) {
        return new WalletQueryService(d);
    }

    @Bean
    WalletUserResolver walletUserResolver(@Value("${ygh.internal-request.hmac-base64}") String e) {
        byte[] k = Base64.getDecoder().decode(e);
        try {
            return new WalletUserResolver(k, Clock.systemUTC());
        } finally {
            Arrays.fill(k, (byte) 0);
        }
    }

    @Bean
    WalletInternalVerifier walletInternalVerifier(@Value("${ygh.internal-request.hmac-base64}") String e) {
        byte[] k = Base64.getDecoder().decode(e);
        try {
            return new WalletInternalVerifier(k);
        } finally {
            Arrays.fill(k, (byte) 0);
        }
    }
}
