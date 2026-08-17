package com.yuegang.zhihui.product.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.product.api.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 该类负责管理产品分类、品牌以及溯源事件的基础增删改查
 */
public final class CatalogService { // 定义目录服务最终类
    private final JdbcTemplate jdbc; // 声明 JDBC 模板
    private final ObjectMapper json; // 声明 JSON 解析器

    public CatalogService(DataSource d, ObjectMapper j) { // 构造函数：注入数据源和 JSON 解析器
        jdbc = new JdbcTemplate(d); // 初始化 JDBC 模板
        json = j; // 初始化 JSON 解析器
    }

    private static CategoryView mapCategory(ResultSet r) throws SQLException { // 映射分类结果集
        Object p = r.getObject("parent_id"); // 尝试获取父分类 ID
        return new CategoryView(
                Long.toString(r.getLong("id")), // ID 转字符串
                p == null ? null : p.toString(), // 父 ID 处理
                r.getString("code"), // 编码
                r.getString("name"), // 名称
                r.getInt("sort_order"), // 排序
                r.getBoolean("enabled"), // 是否启用
                r.getLong("version") // 乐观锁版本
                );
    }

    private static BrandView mapBrand(ResultSet r) throws SQLException { // 映射品牌结果集
        return new BrandView(
                Long.toString(r.getLong("id")), // ID
                r.getString("code"), // 编码
                r.getString("name"), // 名称
                r.getString("logo_url"), // Logo 地址
                r.getBoolean("enabled"), // 是否启用
                r.getLong("version") // 乐观锁版本
                );
    }

    private static Long optional(String x) { // 处理可选的长整型 ID 字符串
        return x == null || x.isBlank() ? null : positive(x); // 为空返回 null，否则校验正数
    }

    private static long positive(String x) {
        try {
            long v = Long.parseLong(x); // 解析数字
            if (v == 0) throw new NumberFormatException(); // 非正数抛出异常
            return v; // 返回长正整型
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 格式错误抛出校验异常
        }
    }

    private static long next() { // 生成随机分布式 ID（模拟）
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE; // 使用 UUID 高位分保证为正
    }

    public CategoryView createCategory(SaveCategoryRequest r) { // 创建产品分类
        long id = next(); // 生成新的分布式 ID
        // 执行插入语句：分类 ID、父 ID、编码、名称、排序权重
        jdbc.update(
                "INSERT INTO product_category(id,parent_id,code,name,sort_order) VALUES(?,?,?,?,?)",
                id,
                optional(r.parentId()),
                r.code(),
                r.name(),
                r.sortOrder());
        return category(id); // 返回新创建的分类视图
    }

    public List<CategoryView> categories(boolean enabled) { // 获取分类列表
        return jdbc.query(
                "SELECT * FROM product_category"
                        + (enabled ? " WHERE enabled=TRUE" : "")
                        + " ORDER BY parent_id,sort_order,id",
                (ResultSet r, int n) -> mapCategory(r)); // 映射结果集到视图
    }

    public BrandView createBrand(SaveBrandRequest r) { // 创建品牌
        long id = next(); // 生成新 ID
        // 执行插入语句：分类 ID、父 ID、编码、名称，排序权重
        jdbc.update(
                "INSERT INTO product_brand(id,code,name,logo_url) VALUES(?,?,?,?)",
                id,
                r.code(),
                r.name(),
                r.logoUrl());
        return brand(id); // 返回新品牌视图
    }

    public List<BrandView> brands(boolean enabled) { // 获取品牌列表
        return jdbc.query(
                "SELECT * FROM product_brand"
                        + (enabled ? " WHERE enabled=TRUE" : "")
                        + " ORDER BY name,id",
                (ResultSet r, int n) -> mapBrand(r)); // 映射结果集到视图
    }

    public TraceEventView addTrace(String sku, TraceEventRequest r) { // 添加到溯源事件
        long id = next(); // 生成事件 ID
        long skuId = positive(sku); // 校验并转换 SKU ID
        // 执行插入语句：包含事件类型，地点，时间，详细信息的 JSON 字符串
        jdbc.update(
                "INSERT INTO product_trace_event(id,sku_id,event_type,location_name,occurred_at,details_json) VALUES(?,?,?,?,?,?)",
                id,
                skuId,
                r.type(),
                r.location(),
                Timestamp.from(r.occurredAt().toInstant()),
                write(r.details()));
        // 返回新创建的溯源事件视图
        // return new TraceEventView(Long.toString(id), sku, r.type(), r.location(), r.occurredAt(),
        // r.details());
        return null;
    }

    public List<TraceEventView> trace(String sku) { // 获取指定 SKU 的溯源链路
        return jdbc.query(
                "SELECT * FROM product_trace_event WHERE sku_id=? ORDER BY occurred_at",
                (r, n) ->
                        new TraceEventView(
                                Long.toString(r.getLong("id")), // 转换事件 ID 为字符串
                                Long.toString(r.getLong("sku_id")), // 转换 SKU ID
                                r.getString("event_type"), // 获取事件类型
                                r.getString("location_name"), // 获取地点
                                r.getTimestamp("occurred_at")
                                        .toInstant()
                                        .atOffset(ZoneOffset.UTC), // 转换时间为 UTC 偏移时间
                                read(r.getString("details_json")) // 解析详细信息 JSON
                                ),
                positive(sku)); // 传入 SKU ID 参数
    }

    private CategoryView category(long id) { // 内部方法: 根据 ID 获取分类
        return jdbc.query(
                "SELECT * FROM product_category WHERE id=?",
                r -> {
                    if (!r.next())
                        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND); // 没找到抛出异常
                    return mapCategory(r); // 返回映射对象
                },
                id);
    }

    private BrandView brand(long id) { // 内部方法：根据 ID 获取品牌
        return jdbc.query(
                "SELECT * FROM product_brand WHERE id=?",
                r -> {
                    if (!r.next())
                        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND); // 没找到抛出异常
                    return mapBrand(r); // 返回映射对象
                },
                id);
    }

    private String write(Object x) {
        try {
            return json.writeValueAsString(x == null ? Map.of() : x); // 处理空值并转换
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 序列化失败抛出异常
        }
    }

    private Map<String, Object> read(String x) { // 将 JSON 字符串解析为 Map
        try {
            return x == null
                    ? Map.of()
                    : json.readValue(x, new TypeReference<Map<String, Object>>() {}); // 反序列化
        } catch (Exception e) {
            return Map.of(); // 解析失败返回空 Map
        }
    }
}
