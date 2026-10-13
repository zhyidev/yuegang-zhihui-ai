package com.yuegang.zhihui.product.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.product.api.ProductStatus;
import com.yuegang.zhihui.product.api.ProductView;
import com.yuegang.zhihui.product.api.SaveProductRequest;
import java.math.BigDecimal;
import java.util.*;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 处理产品创建、分页列表查询、详细信息获取以及状态管理的核心业务类
 */
public final class ProductService { // 定义产品核心服务类，使用 final 防止被继承
    private static final Logger LOG = LoggerFactory.getLogger(ProductService.class); // 初始化日志对象
    private final JdbcTemplate jdbc; // 声明 JDBC 操作模板
    private final TransactionTemplate transactions; // 声明事务操作模板
    private final ProductSearchGateway search; // 声明搜索服务网关，用于处理全文检索

    public ProductService(DataSource dataSource, ProductSearchGateway search) { // 构造函数：注入数据源和搜索网关
        jdbc = new JdbcTemplate(dataSource); // 初始化 JDBC 模板
        // 初始化事务模板，将数据源关联到事务管理器
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        this.search = search; // 初始化搜索网关引用
    }

    public ProductService(DataSource dataSource) { // 无搜索网关的便捷构造：全文检索不可用时走数据库降级
        this(dataSource, null);
    }

    // 辅助方法:生成全局唯一的分布式 ID
    private static long next() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    // 辅助方法:处理可选参数中的ID，为空则返回null
    private static Long blankId(String value) {
        return value == null || value.isBlank() ? null : id(value);
    }

    // 辅助方法：严格解析字符串 ID
    private static long id(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException(); // 不允许非正数
            return id;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR); // 格式解析失败抛出校验异常
        }
    }

    public ProductView create(SaveProductRequest command) { // 创建新产品的方法
        return transactions.execute(
                status -> { // 开启编程式事务
                    long spu = next(), sku = next(); // 分别生成新的 SPU ID 和 SKU ID
                    // 向产品 SPU 表插入基础信息，状态默认为 'DRAFT'（草稿）
                    jdbc.update(
                            "INSERT INTO product_spu(id,category_id,brand_id,name,status) VALUES(?,?,?,?,'DRAFT')",
                            spu,
                            id(command.categoryId()),
                            blankId(command.brandId()),
                            command.name());
                    // 向产品 SKU 表插入详细规格、价格、货币、溯源码等信息，状态为 'DRAFT'
                    jdbc.update(
                            "INSERT INTO product_sku(id,spu_id,sku_code,price,currency,traceability_code,status) VALUES(?,?,?,?,?,?,'DRAFT')",
                            sku,
                            spu,
                            command.skuCode(),
                            command.price(),
                            command.currency(),
                            command.traceabilityCode());

                    int sort = 0; // 图片排序计数器
                    for (String url : command.images()) // 遍历请求中的图片 URL
                    jdbc.update(
                                "INSERT INTO product_image(id,spu_id,sku_id,url,sort_order) VALUES(?,?,?,?,?)",
                                next(),
                                spu,
                                sku,
                                url,
                                sort++);
                    replaceSpecifications(sku, command.specifications()); // 批更新品的规格参数 (Kv映射)
                    return get(Long.toString(sku), false); // 返回新创建的产品详情视图，不限于公开状态
                });
    }

    public List<ProductView> list(
            String category, String keyword, int limit, boolean publicOnly) { // 简化列表查询：不带价格、产地、状态过滤
        return list(category, keyword, null, null, null, null, limit, publicOnly);
    }

    public List<ProductView> list(
            String category,
            String keyword,
            BigDecimal minimumPrice,
            BigDecimal maximumPrice,
            String origin,
            ProductStatus requestedStatus,
            int limit,
            boolean publicOnly) { // 带多重过滤条件的综合列表查询
        // 校验价格参数：最低价、最高价不能为负数，且最低价不能大于最高价
        if ((minimumPrice != null && minimumPrice.signum() < 0)
                || (maximumPrice != null && maximumPrice.signum() < 0)
                || (minimumPrice != null
                        && maximumPrice != null
                        && minimumPrice.compareTo(maximumPrice) > 0))
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        int size = Math.max(1, Math.min(limit, 100)); // 修正分页大小，限制在 1 到 100 之间
        List<String> matchedSkuIds = null; // 用于存储搜索服务匹配到的 ID 列表

        // 如果是公开查询，有关键词且搜索网关可用，则优先使用分布式搜索引擎
        if (publicOnly && keyword != null && !keyword.isBlank() && search != null) {
            try {
                matchedSkuIds = search.search(keyword.strip(), 100); // 从搜索引擎获取前 100 个匹配的 SKU ID
                if (matchedSkuIds.isEmpty()) return List.of(); // 如果搜索服务明确无结果，直接返回空列表
            } catch (RuntimeException unavailable) { // 如果搜索服务不可用，记录警告并降级到数据库 LIKE 过滤
                LOG.warn(
                        "product full-text search unavailable; falling back to transactional database filter");
            }
        }

        // 构建动态 SQL 语句，初始为查询 SKU ID
        StringBuilder sql =
                new StringBuilder(
                        "SELECT s.id FROM product_sku s JOIN product_spu p ON p.id=s.spu_id WHERE 1=1");
        List<Object> arguments = new ArrayList<>(); // 用于存储 SQL 参数的列表
        if (publicOnly)
            sql.append(" AND s.status='PUBLISHED' AND p.status='PUBLISHED'"); // 若仅公开数据，限定状态为
        // 'PUBLISHED'
        if (category != null && !category.isBlank()) { // 如果指定了分类
            sql.append(" AND p.category_id=?"); // 按分类ID过滤
            arguments.add(id(category));
        }

        if (matchedSkuIds != null) { // 如果已经从搜索引擎拿到了匹配 ID 列表
            // 使用 IN 查询子句，并生成对应数量的占位符
            sql.append(" AND s.id IN(")
                    .append(String.join(",", Collections.nCopies(matchedSkuIds.size(), "?")))
                    .append(")");
            matchedSkuIds.stream()
                    .map(Long::parseLong)
                    .forEach(arguments::add); // 将 ID 列表转为 Long 型加入参数
        } else if (keyword != null && !keyword.isBlank()) { // 若未走搜索引擎但有关键词，执行传统的数据库模糊匹配
            sql.append(" AND (p.name LIKE ? OR CONVERT(s.sku_code USING utf8mb4) LIKE ?)");
            String query = "%" + keyword.strip() + "%"; // 前后加 % 进行包含查询
            arguments.add(query);
            arguments.add(query); // 对应名称和 SKU 编码
        }

        if (minimumPrice != null) { // 最低价过滤
            sql.append(" AND s.price=?");
            arguments.add(minimumPrice);
        }
        if (maximumPrice != null) {
            sql.append(" AND s.price<=?");
            arguments.add(maximumPrice);
        }
        if (origin != null && !origin.isBlank()) { // 根据原产地过滤（通过 EXISTS 子句检查关联的批次表）
            sql.append(
                    " AND EXISTS(SELECT 1 FROM product_batch pb WHERE pb.sku_id=s.id AND pb.origin=?)");
            arguments.add(id(category));
        }
        if (!publicOnly && requestedStatus != null) { // 非公开查询下，支持待定状态查询
            sql.append(" AND s.status=?");
            arguments.add(requestedStatus.name());
        }
        // 排序规则：按更新时间排序，再按 ID 降序；最后应用分页大小限制
        sql.append(" ORDER BY p.updated_at DESC,s.id DESC LIMIT ?");
        arguments.add(size);

        // 执行 SQL 查询获取 ID 列表，并遍历列表调用 get 方法获取详细视图数据
        return jdbc.queryForList(sql.toString(), Long.class, arguments.toArray()).stream()
                .map(value -> get(Long.toString(value), publicOnly))
                .toList();
    }

    public ProductView get(String sku, boolean publicOnly) { // 根据 SKU ID 获取单个产品全量详情的方法
        // 构建联表查询语句：SKU + SPU
        String sql =
                "SELECT s.spu_id,s.id,p.category_id,p.brand_id,p.name,s.sku_code,s.price,s.currency,s.status,s.traceability_code,s.version FROM product_sku s JOIN product_spu p ON p.id=s.spu_id WHERE s.id=?"
                        + (publicOnly
                                ? " AND s.status='PUBLISHED' AND p.status='PUBLISHED' "
                                : ""); // 根据参数决定是否应用状态过滤
        return jdbc.query(
                sql,
                result -> {
                    if (!result.next())
                        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND); // 若未找到记录则抛出 404
                    long skuId = result.getLong(2); // 获取主键 ID
                    // 查询关联的图片列表并按序号排序
                    var images =
                            jdbc.queryForList(
                                    "SELECT url FROM product_image WHERE sku_id=? ORDER BY sort_order",
                                    String.class,
                                    skuId);
                    Map<String, String> specifications = new LinkedHashMap<>(); // 存储有序的规格参数 Map
                    // 查询规格参数并填充到 Map 中
                    jdbc.query(
                            "SELECT spec_key,spec_value FROM product_specification WHERE sku_id=? ORDER BY sort_order,spec_key",
                            row -> {
                                specifications.put(row.getString(1), row.getString(2));
                            },
                            skuId);
                    Object brand = result.getObject(4); // 获取品牌 ID 对象（可能为null）
                    // 组装并返回 ProductView Record 对象
                    return new ProductView(
                            Long.toString(result.getLong(1)),
                            Long.toString(skuId),
                            Long.toString(result.getLong(3)),
                            brand == null ? null : brand.toString(),
                            result.getString(5),
                            result.getString(6),
                            result.getBigDecimal(7),
                            result.getString(8),
                            ProductStatus.valueOf(result.getString(9)),
                            images,
                            result.getString(10),
                            result.getLong(11),
                            specifications);
                },
                id(sku));
    }

    public ProductView changeStatus(
            String sku, ProductStatus productStatus, long version) { // 变更产品状态的方法
        long skuId = id(sku); // 校验 ID
        return transactions.execute(
                status -> { // 开启事务
                    // 同步更新 SKU 和 SPU 的状态，并使用 version 进行乐观锁检查
                    int changed =
                            jdbc.update(
                                    "UPDATE product_sku s JOIN product_spu p ON p.id=s.spu_id SET s.status=?,p.status=?,s.version=s.version+1 WHERE s.id=? AND s.version=?",
                                    productStatus.name(),
                                    productStatus.name(),
                                    skuId,
                                    version);
                    if (changed < 1)
                        throw new BusinessException(
                                ErrorCode.BUSINESS_CONFLICT); // 更新行数为表示版本号已被他人终改，微出冲突异带
                    searchJob(skuId); // 更新成功后，向任务表格插入一条记录，触发搜索异步刷新
                    return get(sku, false); // 返回最新的产品详情视图
                });
    }

    // 辅助方法：向搜索同步作业表插入记录
    void searchJob(long skuId) {
        jdbc.update(
                "INSERT INTO product_search_job(id,sku_id) VALUES(?,?)",
                UUID.randomUUID().toString(),
                skuId);
    }

    // 辅助方法：全量替换产品的规格参数
    void replaceSpecifications(long skuId, Map<String, String> values) {
        jdbc.update(
                "DELETE FROM product_specification WHERE sku_id=?", skuId); // 首先物理删除该 SKU 的所有旧规格
        int sort = 0; // 排序计数器
        //
        for (var entry :
                new TreeMap<>(values == null ? Map.<String, String>of() : values).entrySet()) {
            // 插入新的规格 KV，并对值进行去空格处理
            jdbc.update(
                    "INSERT INTO product_specification(sku_id,spec_key,spec_value,sort_order) VALUES(?,?,?,?)",
                    skuId,
                    entry.getKey().strip(),
                    entry.getValue().strip(),
                    sort++);
        }
    }
}
