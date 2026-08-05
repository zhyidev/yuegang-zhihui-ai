package com.yuegang.zhihui.user.api;

import jakarta.validation.constraints.*;

public record UpdateUserProfileRequest( // 定义公共记录类：修改用户个人资料请求 DTO
                                        @NotBlank @Size(max = 80) String displayName, // 校验：展示名称不能为空且最大80个字符
                                        @Size(max = 512) String avatarUrl, // 校验：头像URL连接最大长度512个字符
                                        @Pattern(regexp = "\\+?[0-9 -]{5,31}") String phone, // 校验：手机号格式（可选）
                                        @Email @Size(max = 254) String email, // 校验：邮箱地址必须符合RFC规范且限254个字符
                                        @NotBlank @Pattern(regexp = "[a-z]{2}(?:-[A-Z]{2})?") String locale,  // 校验：语言地区代码，如 zh-CN, en-US
                                        @NotBlank @Size(max = 64) String timezone, // 校验：时区标识符不能为空，如 Asia/Shanghai
                                        @PositiveOrZero long version // 校验：乐观锁版本号
) { // 类体开始
    // 重载构造函数：用于部分更新（仅更新非敏感基础字段）
    public UpdateUserProfileRequest(String displayName, String avatarUrl, String locale, String timezone, long version) {
        this(displayName, avatarUrl, null, null, locale, timezone, version); // 转发到主构造函数，手机和邮箱传null
    } // 构造函数结束

    @Override
    public String toString() { // 重写 toString 方法
        return "UpdateUserProfileRequest[displayName=" + displayName + ", avatarUrl=" + avatarUrl + ", phone=<redacted>, email=<redacted>, locale=" + locale + ", timezone=" + timezone + ", version=" + version + "]"; // 核心：手机号(phone)和邮箱(email)显示为<redacted>，严防日志泄密
    } // 方法结束
}// 类定义结束