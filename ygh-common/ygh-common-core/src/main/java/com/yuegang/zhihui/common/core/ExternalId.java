package com.yuegang.zhihui.common.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 序列化为本文的外部标识符，以确保在 JavaScript 或异构客户端中永远不会丢失精度。
 */
public record ExternalId(String value) { // 使用 Record 包装字符串 ID

    public ExternalId { // 构造校验
        if (value == null || value.isBlank()) { // 校验 ID 不能为空
            throw new IllegalArgumentException("external id must not be blank"); // 抛出异常
        }
        if (!value.equals(value.trim())) { // 校验 ID 两端不能有空格
            throw new IllegalArgumentException("external id must not contain surrounding whitespace"); // 抛出异常
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING) // 用于 JSON 反序列化时将空字符串直接转为对象
    public static ExternalId of(String value) { // 静态工厂
        return new ExternalId(value); // 创建新实例
    }

    @Override
    @JsonValue // 序列化时仅展示内部的字符串值
    public String value() { // 获取内布值
        return value; //返回字符串
    }

    @Override
    public String toString(){ //重写toString
        return value; //返回内部值
    }
}