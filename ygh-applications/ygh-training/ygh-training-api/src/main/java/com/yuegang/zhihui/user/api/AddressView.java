package com.yuegang.zhihui.user.api;

import java.time.OffsetDateTime;

public record AddressView( // 定义公共记录类：地址视图，用于向前端展示数据
                           String id, String label, String recipientName, String recipientPhone,
                           //属性:主键ID、标签(如家/公司)、收货人签名、收货人电话
                           String countryCode, String provinceCode, String provinceName, String cityName,
                           // 属性: 国家代码、省份代码、省份名称、城市名称
                           String districtName, String addressDetail, String postalCode, boolean defaultAddress,
                           // 属性: 区县名称、详细地址、邮编、是否为默认地址
                           long version, OffsetDateTime updatedAt // 属性: 数据版本号 (用于乐观锁)、最后更新时间
) {
} // 类定义结束