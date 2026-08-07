package com.yuegang.zhihui.auth.infrastructure;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.yuegang.zhihui.auth.domain.AccessToken;
import com.yuegang.zhihui.auth.domain.AccessTokenIssuer;
import com.yuegang.zhihui.auth.domain.TokenPrincipal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * 使用 Nimbus 库生成 RSA 签名的 JWT 访问令牌
 */
public final class NimbusAccessTokenIssuer implements AccessTokenIssuer { // 实现令牌签发接口
    private final RsaSigningKeyRing keyRing; // 声明 RSA 密钥环
    private final String issuer; // 签发者名称
    private final String audience; // 受众名称
    private final Duration lifetime; // 令牌有效期时长
    private final Clock clock; // 系统时钟

    public NimbusAccessTokenIssuer( // 构造函数
                                    RsaSigningKeyRing keyRing, String issuer, String audience, Duration lifetime, Clock clock) {
        this.keyRing = Objects.requireNonNull(keyRing, "keyRing must not be null");
        this.issuer = requireText(issuer, "issuer");
        this.audience = requireText(audience, "audience");
        this.lifetime = Objects.requireNonNull(lifetime, "lifetime must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        // 强制约束有效期在 5 到 20 分钟之间，符合高安全架构要求
        if (lifetime.compareTo(Duration.ofMinutes(5)) < 0 || lifetime.compareTo(Duration.ofMinutes(20)) > 0) {
            throw new IllegalArgumentException("access-token lifetime must be between 5 and 20 minutes");
        }
    }

    @Override
    public AccessToken issue(TokenPrincipal principal) { // 核心签发逻辑
        Objects.requireNonNull(principal, "principal must not be null");
        Instant issuedAt = clock.instant(); // 获取当前签发时间
        Instant expiresAt = issuedAt.plus(lifetime); // 计算过期时间
        String jwtId = UUID.randomUUID().toString(); // 生成唯一 JWT ID (jti)
        var claims = new JWTClaimsSet.Builder() // 构建 JWT 载荷
                .issuer(issuer).audience(audience).subject(Long.toString(principal.userId())) // 设置颁发者、受众、主题（用户ID）
                .jwtID(jwtId).issueTime(Date.from(issuedAt)) // 设置 JTI、签发时间
                .notBeforeTime(Date.from(issuedAt)).expirationTime(Date.from(expiresAt)) // 设置生效时间、过期时间
                .claim("account_id", Long.toString(principal.accountId())) // 附加账号ID
                .claim("roles", principal.roles().stream().sorted().toList()) // 附加有序角色列表
                .claim("permissions", principal.permissions().stream().sorted().toList()).build(); // 附加有序权限列表
        var key = keyRing.activeSigningKey(); // 获取密钥环中当前激活的私钥
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.PS256) // 创建头部的 JWT，指定 RS256 算法
                .keyID(key.getKeyID()).type(JOSEObjectType.JWT).build(), claims); // 关键 Key ID 和类型
        try {
            jwt.sign(new RSASSASigner(key.toRSAPrivateKey())); // 使用私钥进行数字签名
            return new AccessToken(jwt.serialize(), jwtId, expiresAt); // 序列化并返回
        } catch (JOSEException signingFailure) { // 捕获签名失败
            throw new IllegalStateException("access token signing failed", signingFailure);
        }
    }

    private static String requireText(String value, String name) { // 文本非空检查辅助
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be null");
        return value;
    }
}