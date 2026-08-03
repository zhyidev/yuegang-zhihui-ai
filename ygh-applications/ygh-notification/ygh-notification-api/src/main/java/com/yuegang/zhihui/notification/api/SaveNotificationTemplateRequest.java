package com.yuegang.zhihui.notification.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 这个 Record 用于封装创建或更新通知模板的请求参数 */
public record SaveNotificationTemplateRequest( // 定义保存通知模板的请求对象
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,63}") String code, // 模板编码：必填，且必须是大写字母开头
        @NotBlank @Size(max = 200) String titleTemplate, // 标题模板：必填，最大长度限制为 200 个字符
        @NotBlank @Size(max = 10000) String contentTemplate, // 内容模板：必填，最大长度限制为 10000 个字符
        @NotBlank @Pattern(regexp = "IN_APP") String channel, // 发送渠道：目前仅支持值"IN_APP"(站内信)
        boolean enabled, // 是否启用模板的开关
        @PositiveOrZero long version) { } // 版本号：必须大于或等于 0 的整数，用于更新时的乐观锁控制