package com.yuegang.zhihui.auth.domain;

import java.time.Duration;

/** 验证码存储接口 */
public interface CaptchaChallengeStore { // 验证码挑战存储接口
    void save(String challengeId, String answerHash, Duration ttl); // 保存挑战 ID、哈希值和存活时间
    boolean consume(String challengeId, String presentedAnswerHash); // 消费（验证并删除）验证码，返回验证结果
}