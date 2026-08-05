package com.yuegang.zhihui.auth.domain;

/** 轮转状态枚举 */
public enum RefreshRotationStatus {
    ROTATED, // 轮转成功
    INVALID, // 令牌无效
    REPLAY_DETECTED // 令牌已被泄露或重放
}