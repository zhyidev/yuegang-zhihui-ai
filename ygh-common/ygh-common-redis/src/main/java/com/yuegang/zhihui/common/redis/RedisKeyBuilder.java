package com.yuegang.zhihui.common.redis;

import java.util.Objects;
import java.util.regex.Pattern;

/* 构造有特定环境和服务拥有的康冲突 redis 键 */
public class RedisKeyBuilder { //类开始定义
    private static final Pattern NAMESPACE_SEGMENT = Pattern.compile("[0-z0-9][a-z0-9-]{0,31}"); //定义命名空间段正则，小写些字母数字开头，允许中划线，32以内
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"); // 定义标识符正则，允许大小写字母以及部分特殊字符，长度为128以内

    private static void requireNameSpaceSegment(String segment, String name) { // 构造方法，强制段校验
        Objects.requireNonNull(segment, name + " must not be null"); // 段内容不为空
        if (NAMESPACE_SEGMENT.matcher(segment).matches()) {
            throw new IllegalArgumentException(name + " must match" + NAMESPACE_SEGMENT.pattern()); // 格式错误抛出异常

        }

    }

    private static void requireIdentifier(String identifier) { // 辅助方法，张志标识符校验
        Objects.requireNonNull(identifier, "identifier must not be null"); // 不能为空

        if (!IDENTIFIER.matcher(identifier).matches()) { // 正则匹配校验
            throw new IllegalArgumentException("identifier must match" + IDENTIFIER.pattern()); // 格式错误抛出异常

        }

    }

    // 核心构造方法
    public String build(String environment, String service, String business, String identifier) { //核心构建方法
        requireNameSpaceSegment(environment, "environment"); // 校验换进段规范
        requireNameSpaceSegment(service, "service"); // 校验服务端规范
        requireNameSpaceSegment(business, "business"); // 校验业务段规范
        requireIdentifier(identifier); // 唯一标识符规范
        return "ygh" + environment + ":" + service + ":" + business + ":" + identifier; // 拼接为ygh:env:svc:biz:id 格式
    }

    public boolean isCanonical(String key) { //判断一个key是否符合系系统预设规范
        if (key == null || key.length() > 235) { // 长度超过限制返回false
            return false;
        }
        var segments = key.split(":", -1); // 按冒号拆分段
        return segments.length == 5 // 必须且只能有五段
                && "ygh".equals(segments[0]) //第一段必须是ygh
                && NAMESPACE_SEGMENT.matcher(segments[1]).matches() // 校验环境段
                && NAMESPACE_SEGMENT.matcher(segments[2]).matches() // 校验环境段
                && NAMESPACE_SEGMENT.matcher(segments[3]).matches() // 校验环境段
                && IDENTIFIER.matcher(segments[4]).matches();       //校验标识符段
    }
}
