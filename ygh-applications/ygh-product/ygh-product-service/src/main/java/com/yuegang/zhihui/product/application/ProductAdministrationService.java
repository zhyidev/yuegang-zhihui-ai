package com.yuegang.zhihui.product.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.product.api.ProductBatchView;
import com.yuegang.zhihui.product.api.ProductView;
import com.yuegang.zhihui.product.api.SaveProductBatchRequest;
import com.yuegang.zhihui.product.api.UpdateProductRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 处理产品信息更新、批次管理以及审计事件的发布 */
public class ProductAdministrationService { // 定义产品管理服务类
    private final JdbcTemplate jdbc; // 数据库操作模板
    private final ProductService products;
    private final ObjectMapper json;

    public ProductAdministrationService(DataSource dataSource, ProductService products, ObjectMapper json) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.products = products;
        this.json = json;
    }

    @Transactional // 开启事务: 创建生产批次
    public ProductBatchView batch(String sku, SaveProductBatchRequest command) {
        long skuId = id(sku); // 获取长整型 SKU ID
        // 校验逻辑：过期日期不能再生产日期之前
        if (command.producedOn() != null && command.expiresOn() != null && command.expiresOn().isBefore(command.producedOn()))
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        long batchId = next(); // 生成批次 ID
        // 插入批次记录：包含批次号，产地，证明 URL，日期，溯源描述
        jdbc.update("INSERT INTO product_batch(id,sku_id,batch_no,origin,proof_url,produced_on,expires_on,trace_description) VALUES(?,?,?,?,?,?,?,?)",
                batchId, skuId, command.batchNo(), command.origin(), command.proofUrl(),
                command.producedOn(), command.expiresOn(), command.traceDescription());

        // 记录批次创建事件
        event(skuId, "PRODUCT_BATCH_CREATED", Map.of("skuId", sku, "batchId", Long.toString(batchId), "batchNo", command.batchNo()));
                products.searchJob(skuId); // 标记需要重新构建搜索索引
        // 返回批次视图
        return new ProductBatchView(Long.toString(batchId), sku, command.batchNo(),
                command.origin(), command.proofUrl(), command.producedOn(), command.expiresOn(),
                command.traceDescription());
    }

    @Transactional // 开启事务：更新产品信息
    public ProductView update(String sku, UpdateProductRequest command) {
        long skuId = id(sku); // 校验 SKU ID
        // 查询更新前的价格和 SPU ID，用于对比和后续操作
        Object[] before = jdbc.query("SELECT s.price,s.spu_id FROM product_sku s WHERE s.id=?",  result -> {
            if (!result.next()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND); // 未找到抛出异常
            return new Object[]{result.getBigDecimal(1), result.getLong(2)}; // 返回价格和 SPU ID
        }, skuId);
        // 执行联表更新：更新 SKU 和关联的 SPU 基础信息，增加版本号实现乐观锁
        int changed = jdbc.update("UPDATE product_sku s JOIN product_spu p ON p.id=s.spu_id SET p.category_id=?,p.brand_id=?,p.name=?,p.description=?,s.price=?,s.currency=?,s.traceability_code=?,s.version=s.version+1,p.version=p.version+1 WHERE s.id=? AND s.version=?",
                id(command.categoryId()), blank(command.brandId()), command.name(), command.description(), command.price(), command.currency(),
                command.traceabilityCode(), skuId, command.version());
        if (changed < 1) throw new BusinessException(ErrorCode.BUSINESS_CONFLICT); // 版本号不一致抛出冲突异常
        jdbc.update("DELETE FROM product_image WHERE sku_id=?", skuId); // 先清空该 SKU 的旧图片
        int sort = 0; // 排序计数器
        for (String url : command.images()) { // 重新插入新的图片列表
            jdbc.update("INSERT INTO product_image(id,spu_id,sku_id,url,sort_order) VALUES(?,?,?,?,?)",
                    next(), before[1], skuId, url, sort++);
        }
        products.replaceSpecifications(skuId, command.specifications()); // 批量替换产品规格

        // 如果价格发生变动，记录道价格历史审计表
        if (((java.math.BigDecimal) before[0]).compareTo(command.price()) != 0) {
            jdbc.update("INSERT INTO product_price_history(id,sku_id,old_price,new_price,currency) VALUES(?,?,?,?,?)",
                    next(), skuId, before[0], command.price(), command.currency());
        }

        // 发布本地消息事件到 Outbox，用于同步到其他微服务
        event(skuId, "PRODUCT_UPDATED", Map.of("skuId", sku, "price", command.price(), "currency", command.currency()));
        products.searchJob(skuId); // 触发异步搜索索引更新服务
        return products.get(sku, false); // 返回最新的产品详情视图
    }

    private List<ProductBatchView> batches(String sku) { // 查询至 SKU 的所有历史批次
        return jdbc.query("SELECT id,sku_id,batch_no,origin,proof_url,produced_on,expires_on,trace_description FROM product_batch WHERE sku_id=? ORDER BY created_at DESC",
                (result, row) -> new ProductBatchView(
                        Long.toString(result.getLong(1)), Long.toString(result.getLong(2)),
                        result.getString(3), result.getString(4), result.getString(5),
                        date(result.getDate(6)), date(result.getDate(7)), result.getString(8)
                ), id(sku));
    }

    private void event(long skuId, String type, Object payload) { // 将领域事件存入 Outbox 类
        jdbc.update("INSERT INTO product_outbox(id,aggregate_id,event_type,payload_json) VALUES(?,?,?,?)",
                UUID.randomUUID().toString(), Long.toString(skuId), type, write(payload));
    }

    private String write(Object value) {// F列化辅助方法 no usages
        try {
            return json.writeValueAsString(value);
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static LocalDate date(java.sql.Date value) { //  SQL to Java// 日期
        return value == null ? null : value.toLocalDate();
    }

    private static Long blank(String value) {//处理可能为空白的 ID 字符串
        return value == null || value.isBlank() ? null : id(value);
    }

    private static long id(String value) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (Exception failure) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private static long next() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }
}
