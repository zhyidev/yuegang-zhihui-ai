package com.yuegang.zhihui.auth.domain;

/**
 * 令牌签发接口
 */
public interface AccessTokenIssuer { // 定义访问令牌签发器接口
    AccessToken issue(TokenPrincipal principal);
}
