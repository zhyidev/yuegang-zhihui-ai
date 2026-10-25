INSERT INTO product_brand (id, code, name, logo_url, enabled, version)
VALUES (9001, 'YGH_DEMO', '粤港甄选自营', NULL, TRUE, 0);

INSERT INTO product_spu (id, category_id, brand_id, name, description, status, version)
VALUES (9101, 101, 9001, '供港冰鲜走地鸡', '演示商品', 'PUBLISHED', 0);

INSERT INTO product_sku (id, spu_id, sku_code, price, currency, traceability_code, status, version)
VALUES (9201, 9101, 'YGH-HK-CHICKEN-01', 128.00, 'CNY', 'TRACE-YGH-0001', 'PUBLISHED', 0);

INSERT INTO product_specification (sku_id, spec_key, spec_value, sort_order)
VALUES (9201, '产地', '广东惠州', 0), (9201, '规格', '1.2kg/只', 1);

INSERT INTO product_image (id, spu_id, sku_id, url, sort_order)
VALUES (9301, 9101, 9201, 'https://example.com/demo-chicken.png', 0);
