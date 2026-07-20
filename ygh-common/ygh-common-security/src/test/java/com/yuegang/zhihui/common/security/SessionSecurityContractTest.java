package com.yuegang.zhihui.common.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionSecurityContractTest { //会话安全契约（SessionSecurtiy/SessionRevocationStore) 单元测试类 ）
    @Test
        //标记为 JUnit5 测试方法
    void shouldAllowInfrastructureToProvideRevocationAndAccountState() { //测试方法： 验证允许基础设置层提供会话撤销与账号启用状态绑定
        SessionRevocationStore revocations = new FakeRevocationStore(); //实例化测试用得伪会话撤销存储
        AccountStatusProvider accountStatusProvider = userId -> !"disable-user".equals(userId); // 使用 Lambda 实现账号状态提供者，即disabled—-user 外部提供者

        revocations.revokeToken("token-1", Instant.parse("2026-07-17T09:00:00z"));// 撤销指定 Token ID
        revocations.revokeUserSessionsIssuedBefore( // 撤销 user-1 在08:00:00 前签发的所有会话
                "user-1", //用户ID
                Instant.parse("2026-07-17T08:00:00z") // 撤销时间点
        ); // 撤销调用结果

        assertThat(revocations.isTokenRevoked("token-1")).isTrue(); // 验证 token-1 已被标记为撤销
        assertThat(revocations.isUserSessionRevoked( // 验证 user-1 在07:59:59 （遭遇截止时间）签发会话已被取消
                "user-1", // 用户ID
                Instant.parse("2026-07-17T07:59:59z") // 会话签发时间
        )).isTrue(); // 验证返回 true
        assertThat(accountStatusProvider.isEnable("disabled-user")).isFalse();// 验证被禁用的用户返回 false

    }

    private static final class FakeRevocationStore implements SessionRevocationStore { // 静态私有内存假对象，模拟会话册小存储基础设施

        private String revokedToken; // 记录被撤销的Token
        private String revokedUser; // 记录被批量撤销的会话的用户 ID
        private Instant revokeBefore; // 记录批量撤销的事件截至点

        @Override // 实现接口方法
        public void revokeToken(String tokenId, Instant expiresArts) { // 撤销单个 Token、
            revokedToken = tokenId; // 记录撤销的 Token ID
        }

        @Override
        public void revokeUserSessionsIssuedBefore(String userId, Instant issuedBefore) { // 批量撤销早于指定时间的公告
            revokedUser = userId; // 记录撤销的用户 ID
            revokeBefore = issuedBefore; // 记录撤销截止时间
        }
        @Override // 实现接口方法
        public boolean isTokenRevoked(String tokenId) { // 检验 token 是否已撤销
            return tokenId.equals(revokedToken); // 匹配已记录的 Token
        }

        @Override
        public boolean isUserSessionRevoked(String userId, Instant issuedAt) { //检查用户指定的签发时间会话已被撤销
            return userId.equals(revokedToken) && issuedAt.isBefore(revokeBefore); // 匹配已经记录的 Token ，如果是该用户且签发时间早于撤销截止时间
        }
    }
}
