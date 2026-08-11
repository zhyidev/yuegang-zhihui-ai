package com.yuegang.zhihui.order.api;

import jakarta.validation.constraints.NotBlank;

public record AddressSnapshot(@NotBlank String recipientName, @NotBlank String recipientPhone,
                              @NotBlank String countryCode, String provinceCode, @NotBlank String provinceName,
                              @NotBlank String cityName, @NotBlank String districtName, @NotBlank String addressDetail,
                              String postalCode) {
}
