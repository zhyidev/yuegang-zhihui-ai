package com.yuegang.zhihui.admin.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuegang.zhihui.admin.security.AdminUserVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Base64;

@Configuration(proxyBeanMethods = false)
class AdminConfiguration {
    @Bean
    AdminDashboardService adminDashboardService(@Value("${ygh.admin.service-health-urls}") String u) {
        return new AdminDashboardService(u);
    }

    @Bean
    AuditQueryService auditQueryService(@Value("${ygh.admin.loki-base-url}") String u, ObjectMapper j) {
        return new AuditQueryService(u, j);
    }

    @Bean
    AdminUserVerifier adminUserVerifier(@Value("${ygh.internal-request.hmac-base64}") String e) {
        byte[] k = Base64.getDecoder().decode(e);
        try {
            return new AdminUserVerifier(k);
        } finally {
            Arrays.fill(k, (byte) 0);
        }
    }
}
