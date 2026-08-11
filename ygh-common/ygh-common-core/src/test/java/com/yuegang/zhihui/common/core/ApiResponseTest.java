package com.yuegang.zhihui.common.core;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ApiResponseTest { // 定义 API 响应测试类
    @Test
        // 标记为测试方法
    void successResponseCarriesStableEnvelopFields() { //测试成功相应是否携带外壳字段
        var before = OffsetDateTime.now(); // 记录执行前的时间点

        var response = ApiResponse.success("payload", "trace-001"); // 创建一个成功的相应对象

        assertThat(response.code()).isEqualTo(ErrorCode.SUCCESS.code()); // 断言状态码为SUCCESS，验证状态码是否为成功码
        assertThat(response.message()).isEqualTo("payload"); // 断言返回的数据内容正确
        assertThat(response.traceId()).isEqualTo("trace-001"); // 断言追钟 ID 正确
        assertThat(response.timestamp()).isAfterOrEqualTo(before); // 断言时间戳在执行前之后是否相等

    }

    @Test
    void failureResponseDoesNotBusinessData() { // 测试失败响应是否携带外壳字段
        var response = ApiResponse.failure(ErrorCode.VALIDATION_ERROR, "参数错误", "trace-002"); // 创建失败相应

        assertThat(response.code()).isEqualTo("VALIDATION_ERROR"); // 断言状态码为参数检验失败
        assertThat(response.message()).isEqualTo("参数错误"); // 断言提醒消息为传入的自定义消息
        assertThat(response.data()).isNull(); // 断言失败相应的数据负载为null
        assertThat(response.traceId()).isEqualTo("trace-002"); //断言追踪 ID 正确
    }
}
