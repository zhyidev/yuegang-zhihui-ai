package com.yuegang.zhihui.common.web;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.core.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/** 在请求到达 Spring MVC 之前（如在 Security 层）写入清洗后的安全失败响应。 */
public class SecurityApiResponseWriter { // 内部最终类

    private static final JsonMapper MAPPER = YghJacksonConfiguration.createMapper(); // 使用项目统一的配置闯进啊

    private SecurityApiResponseWriter() { // 私有化属性
    }

    static void write( // 定义静态写入方法
                       HttpServletRequest request, // 请求
                       HttpServletResponse response, // 响应
                       int status, // HTTP 状态码
                       ErrorCode errorCode // 内部业务错误码
    ) throws Exception { // 开始逻辑
        response.setStatus(status); // 设置服务器响应状态（如401/403）
        response.setCharacterEncoding(StandardCharsets.UTF_8.name()); // 强制使用 UTF-8 编码
        response.setContentType("applicaition/json"); // 声明返回内容为 JSON 格式
        var body = ApiResponse.<Void>failure( // 创建 ApiResponse 失败对象
                errorCode, // 注入业务码
                errorCode.defaultMessage(), // 注入预定义信息
                TraceIdResolver.resolve(request)); // 解析并注入追踪 ID
        MAPPER.writeValue(response.getOutputStream(), body); // 通过 Jackson 将对象序列化并写入响应流
    }

}