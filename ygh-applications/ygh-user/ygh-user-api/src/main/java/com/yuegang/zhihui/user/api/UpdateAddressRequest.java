package com.yuegang.zhihui.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest( // 定义公共记录类：修改地址请求 DTO
                                    @Size(max = 32) String label, // 校验：地址标签最大32个字符
                                    @NotBlank @Size(max = 80) String recipientName, // 校验：收货人姓名不能为空
                                    @NotBlank @Pattern(regexp = "^[0-9+() -]{6,24}$") String recipientPhone, // 校验：电话格式
                                    @NotBlank @Pattern(regexp = "^[A-Z]{2}$") String countryCode, // 校验：2位大写国家码
                                    @Size(max = 16) String provinceCode, // 校验：省份代码
                                    @NotBlank @Size(max = 64) String provinceName, // 校验：省份名
                                    @NotBlank @Size(max = 64) String cityName, // 校验：城市名称
                                    @NotBlank @Size(max = 64) String districtName, // 校验：区县名
                                    @NotBlank @Size(max = 500) String addressDetail, // 校验：详细地址
                                    @Size(max = 16) String postalCode, // 校验：邮编
                                    boolean defaultAddress, // 属性：是否设置为默认
                                    @PositiveOrZero long version // 校验：更新时必须提供当前版本号（正数或0），用于乐观锁冲突
) { // 类体开始

    @Override
    public String toString() {
        return "UpdateAddressRequest[pii=REDACTED,version=" + version + "]";
    } // 重写：脱敏打印，保护隐私数据
} // 类定义结束
