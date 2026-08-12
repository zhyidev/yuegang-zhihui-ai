package com.yuegang.zhihui.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 公共不可变投影：持久化实体（Entity），永远不会跨越服务边界。
 */
public record UserProfileView( // 定义公共记录类：用户资料视图
                               @NotBlank String userId, // 校验：用户唯一ID不能为空
                               @NotBlank @Size(max = 80) String displayName, // 校验：展示名称
                               @Size(max = 512) String avatarUrl, // 校验：头像链接
                               String phone, // 属性：手机号（脱敏或加密存储的值）
                               String email, // 属性：电子邮箱
                               String locale, // 属性：多语言设置
                               String timezone, // 属性：时区设置
                               long version // 属性：数据版本
) { // 类体开始
    // 快捷构造函数：用于基础注册场景的默认展示
    public UserProfileView(String userId, String displayName, String avatarUrl, long version) {
        this(userId, displayName, avatarUrl, null, null, "zh-CN", "Asia/Shanghai", version); // 默认中文及上海时区
    } // 构造函数结束

    public UserProfileView { // 紧凑构造函数，对数据一致性进行终极校验
        if (userId == null || !userId.matches("[1-9][0-9]{0,18}")) { // 校验用户ID必须为正整数格式的字符串
            throw new IllegalArgumentException("userId must be positive decimal identifier"); // 格式错误抛异常
        } // 校验结束
        if (version < 0) throw new IllegalArgumentException("version must not be negative"); // 校验版本号不能为负

    } // 构造逻辑结束

    @Override
    public String toString() { // 重写 toString 方法
        return "UserProfileView[userId=" + userId + ", displayName=" + displayName + ", avatarUrl=" + avatarUrl
                + ", phone=<redacted>, email=<redacted>, locale=" + locale + ", timezone=" + timezone
                + ", version=" + version + "]"; // 安全策略：在对象序列化打印时，再次强调遮蔽联系方式信息
    } // 方法结束

} // 类定义结束
