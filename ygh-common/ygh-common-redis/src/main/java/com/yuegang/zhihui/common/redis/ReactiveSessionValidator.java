package com.yuegang.zhihui.common.redis;

import reactor.core.publisher.Mono;

@FunctionalInterface // 标识这是一个函数式接口
public interface ReactiveSessionValidator {

    Mono<Boolean> valid(long accountId, String jwtId);
}
