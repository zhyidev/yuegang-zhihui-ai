package com.yuegang.zhihui.system.api;

public record SystemSettingView(String key, String value, String valueType, boolean secret,
                                long version) { //设置项键、设置值、值类型（如STRING)、是否为敏感值（密文)、版本号
}
