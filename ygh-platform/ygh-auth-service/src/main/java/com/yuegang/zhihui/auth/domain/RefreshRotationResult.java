package com.yuegang.zhihui.auth.domain;

/** 刷新令牌轮转结果 */
public record RefreshRotationResult(RefreshRotationStatus status, long accountId) { // 刷新令牌更新后的状态结果

    public RefreshRotationResult { // 构造检测
        if (status == RefreshRotationStatus.ROTATED && accountId <= 0) { // 成功轮转时必须有关联账号 ID
            throw new IllegalArgumentException("rotated result requires accountId");
        }
    }

    public static RefreshRotationResult invalid(){ return new RefreshRotationResult(RefreshRotationStatus.INVALID, 0); } // 静态工厂:无效令牌
    public static RefreshRotationResult replay(){ return new RefreshRotationResult(RefreshRotationStatus.REPLAY_DETECTED, 0); } // 旧的令牌重复使用

}