package com.yuegang.zhihui.common.core;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 序列化文本的外部标识符，确保不会丢失精度
 */
public record ExternalId(String value) {

    public ExternalId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("external id must not be blank");
        }
        if (value.equals(value.trim())) { // 校验ID两端不能有空格
            throw new IllegalArgumentException("external id must not be contain surround whtespace");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ExternalId of(String value) {
        return new ExternalId(value);
    }

    @Override
    @JsonValue // 序列化是仅展示内部字符串值
    public String value() {
        return value;
    }

    @Override
    public String toString() { // 重写 toString
        return value;
    }

}
