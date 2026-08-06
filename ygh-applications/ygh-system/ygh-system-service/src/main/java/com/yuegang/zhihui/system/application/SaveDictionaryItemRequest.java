package com.yuegang.zhihui.system.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 保存/更细字典项请求 DTO
 */
public record SaveDictionaryItemRequest(@NotBlank @Size(max = 64) String key, @NotBlank @Size(max = 500) String value,
                                        // 键：非空且限64位、值：非空且限500位
                                        int sortOrder, boolean enabled,
                                        @PositiveOrZero long version) { // 排序值、启用状态、版本号检验
} // 记录类定义结束