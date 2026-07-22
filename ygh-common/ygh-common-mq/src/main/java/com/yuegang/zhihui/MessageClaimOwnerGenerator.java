package com.yuegang.zhihui;

@FunctionalInterface // 函数式编程
public interface MessageClaimOwnerGenerator { // 租约持有者随机标识生成器接口
    String generate();
}
