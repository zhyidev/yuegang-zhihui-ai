package com.yuegang.zhihui.search.infrastructure;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class ElasticsearchRestClientFactory {
    private ElasticsearchRestClientFactory() {
    }

    public static RestClient create(String baseUrl, String username, String password) {
        if (username == null || username.isBlank()) {
            username = System.getenv().getOrDefault("YGH_ELASTICSEARCH_USERNAME", "");
            password = System.getenv().getOrDefault("YGH_ELASTICSEARCH_PASSWORD", "");
        }
        var builder = RestClient.builder().baseUrl(baseUrl);
        if (username != null && !username.isBlank()) {
            String credentials = username + ":" + (password == null ? "" : password);
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
        return builder.build();
    }
}
