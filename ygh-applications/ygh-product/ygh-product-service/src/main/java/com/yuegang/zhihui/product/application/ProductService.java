package com.yuegang.zhihui.product.application;

import com.yuegang.zhihui.common.core.BusinessException;
import com.yuegang.zhihui.common.core.ErrorCode;
import com.yuegang.zhihui.product.api.ProductView;
import com.yuegang.zhihui.product.api.SaveProductRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.UUID;

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

    public ProductView create(SaveProductRequest command) { // 创建新产品的方法
        return transactions.execute( status -> { // 开启编程式事务
            long spu = next(), sku = next(); // 分别生成新的 SPU ID 和 SKU ID
        }
    }

    // 辅助方法:生成全局唯一的分布式 ID
    private static long next() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    //辅助方法:处理可选参数中的ID，为空则返回null
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

}