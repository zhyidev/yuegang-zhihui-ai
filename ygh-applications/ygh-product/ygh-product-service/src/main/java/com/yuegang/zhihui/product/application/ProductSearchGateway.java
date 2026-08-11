package com.yuegang.zhihui.product.application;

import com.yuegang.zhihui.common.core.ApiResponse;
import com.yuegang.zhihui.common.security.InternalServiceSignature;
import com.yuegang.zhihui.search.api.ProductSearchRequest;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 实时调用搜索微服务，通过关键词检索出匹配产品 SKU ID 列表
 */
public final class ProductSearchGateway { // 定义产品搜索网关类，使用final关键字防止被继承
    private static final String PATH = "/internal/v1/search/products"; // 定义搜索服务内部接口的固定访问路径常量
    private final RestClient client; // 声明底层的HTTP客户端实例，用于发起网络请求
    private final InternalServiceSignature signatures; // 声明内部服务间签名的工具实例，用于安全校验

    public ProductSearchGateway(String baseUrl, byte[] secret) { // 构造函数：初始化时传入搜索服务的基地址和共享密钥
        client = RestClient.builder().baseUrl(baseUrl).build(); // 使用建造者模式创建并配置 RestClient
        signatures = new InternalServiceSignature(secret, Clock.systemUTC(), Duration.ofSeconds(30)); // 初始化签名器，使用UTC时间并设置30秒的有效期
    }

    public List<String> search(String keyword, int limit) { // 用于远程关键词检索的方法
        Instant now = Instant.now(); // 获取当前的时间戳
        // 构建签名所需的元数据对象，包含自身服务名，请求动作，请求路径和当前时间
        var metadata = new InternalServiceSignature.Metadata("ygh-product-service", "POST", PATH, now);

        ApiResponse<List<Map<String, Objects>>> response = client.post().uri(PATH)
            .header("X-YGH-Service", "ygh-product-service") // 设置发起方服务标识头
            .header("X-YGH-Service-Signature", signatures.sign(metadata)) // 设置根据元数据生成的HMAC签名头
            .body(new ProductSearchRequest(keyword, limit)).retrieve().body(ApiResponse.class);

        // 如果响应为空或者响应体中的data字段为空，则返回一个不可变的空列表
        if (response == null || response.data() == null) return List.of();

        // 使用Java Stream API处理返回的数据流
        return response.data().stream()
            // 从Map中提取键为"skuId"的值，并安全地转换为字符串（若不存在则转为空串）
            .map(value -> Objects.toString(value.get("skuId"), ""))
            // 通过正则表达式过滤结果：必须是1-19位的数字，且第一位不能为0（匹配长整型ID格式）
            .filter(value -> value.matches("[1-9][0-9]{0,18}")).toList(); // 收集结果并转为List返回
    }
}
