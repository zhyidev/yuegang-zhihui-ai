package com.yuegang.zhihui.common.security;

@FunctionalInterface // 表示函数式接口
public interface SecurityAuditPublisher { // 定义安全审计事件发布窗口，解耦具体发布逻辑(如MQ 或HTTP)
    void publish(SecurityAuditEvent event); //定义发布审计事件的抽象方法
}
