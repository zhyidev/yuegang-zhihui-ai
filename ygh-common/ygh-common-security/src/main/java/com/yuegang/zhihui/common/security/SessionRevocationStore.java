package com.yuegang.zhihui.common.security;

import java.time.Instant;

public interface SessionRevocationStore { // 定义会话撤销存储接口，用于管理 Token 黑名单和强制下线逻辑 no usages

    void revokeToken(String tokenId, Instant expiresArts); // 将特定的 Token ID 撤销，并指定在 Redis 中存储的过期事假 no usages

    void revokeUserSessionsIssuedBefore(String userId, Instant issuedBefore); // 撤销该用户在指定时间戳之前生成的所有会话 no usages

    boolean isTokenRevoked(String tokenId); // 检查单个 Token ID 是否在黑名单中 no usages

    boolean isUserSessionRevoked(String userId, Instant issuedAt); // 根据会话签发时间判断该用户的这一笔会话是否已被全局撤销 no usages
}