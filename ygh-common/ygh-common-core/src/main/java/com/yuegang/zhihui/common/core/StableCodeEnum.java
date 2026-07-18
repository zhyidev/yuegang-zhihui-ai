package com.yuegang.zhihui.common.core;

import com.fasterxml.jackson.annotation.JsonValue;

public interface StableCodeEnum {

    @JsonValue
    String code();

    String displayName();
}
