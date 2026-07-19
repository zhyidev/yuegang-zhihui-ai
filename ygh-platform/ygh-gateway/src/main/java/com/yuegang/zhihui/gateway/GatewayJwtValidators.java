package com.yuegang.zhihui.gateway;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

import java.util.List;

/**
 * JWT 令牌验证器工厂。
 *
 * <p>创建强制性的发行者（Issuer）、过期时间（Expiry）、
 * 受众（Audience）验证器链，确保网关接收的 JWT 令牌合法有效。</p>
 *
 * <p>验证链路：</p>
 * <ol>
 *   <li>Spring Security 默认验证（exp 过期时间、iss 发行者）；</li>
 *   <li>自定义受众（aud）验证 —— 令牌的 aud 声明必须包含配置的受众值。</li>
 * </ol>
 *
 * @author yuegang-zhihui
 * @since 1.0.0
 */
final class GatewayJwtValidators {

    /** 私有构造器，工具类不可实例化 */
    private GatewayJwtValidators() {}

    /**
     * 创建组合 JWT 验证器。
     *
     * @param issuer   令牌发行者（iss），不能为空
     * @param audience 令牌受众（aud），不能为空
     * @return 组合验证器实例
     * @throws IllegalArgumentException 如果 issuer 或 audience 为空
     */
    static OAuth2TokenValidator<Jwt> create(String issuer, String audience) {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("issuer must not be blank");
        }
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("audience must not be blank");
        }

        // Spring Security 默认验证器：验证 exp（过期时间）和 iss（发行者）
        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(issuer);

        // 自定义受众验证器：令牌的 aud 声明必须包含配置的受众值
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD,
                aud -> aud != null && aud.contains(audience));

        // 合并多个验证逻辑（全部通过才放行）
        return new DelegatingOAuth2TokenValidator<>(defaults, audienceValidator);
    }
}
