package com.yuegang.zhihui.product.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;

/** 处理产品信息更新、批次管理以及审计事件的发布 */
public class ProductAdministrationService { // 定义产品管理服务类
    private final JdbcTemplate jdbc; // 数据库操作模板
    private final ProductService products;
    private final ObjectMapper json;
}