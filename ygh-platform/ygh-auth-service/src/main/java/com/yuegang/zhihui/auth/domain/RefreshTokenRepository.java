package com.yuegang.zhihui.auth.domain;

import java.time.Instant;

/** 刷新令牌仓储接口 */
public interface RefreshTokenRepository { // 刷线令牌持久化接口
    void insertInitial(long accountId, String family, NewRefreshToken token); // 插入首个刷新推理 (开启新的令牌秩)
    // 执行令牌轮转：作废旧哈希并存入新哈希，返回处理结果（用于防御令牌盗用）
    RefreshRotationResult rotate(String presentedHash, NewRefreshToken replacement, Instant now);
    void revokeFamilyByTokenHash(String presentedHash, Instant now, String reason); // 撤销整个令牌族（当发现重放攻击时执行，强制用户下线）
}