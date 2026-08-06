package com.yuegang.zhihui.auth.domain;

import java.util.Optional;

@FunctionalInterface // 声明为函数式接口
public interface LoginAccountRepository { // 登录账号仓储接口
    Optional<LoginAccount> findByPrincipal(String normalizedPrincipal); // 根据标准化凭证（用户名/邮箱等）查找账号

    default Optional<LoginAccount> findByAccountId(long accountId) { // 默认实现：根据账号 ID 查找
        throw new UnsupportedOperationException("findByAccountId is not implemented");
    }

    default LoginAccount create(long accountId, long userId, String normalizedPrincipal, String accountType, PasswordDigest passwordDigest) {
        throw new UnsupportedOperationException("create is not implemented");
    }
}