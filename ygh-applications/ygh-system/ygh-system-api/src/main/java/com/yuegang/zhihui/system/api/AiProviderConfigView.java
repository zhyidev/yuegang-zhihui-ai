package com.yuegang.zhihui.system.api;

import java.time.OffsetDateTime;

/**
 * AI 供应商匹配视图对象（用于前端展示）
 */
public record AiProviderConfigView( // 定义共有的记录类（Record）：AI供应商配置视图
                                    String provider, // 字段：供应商标识名称（如：UBALDO_ARK）
                                    String baseUrl, // 字段：API 基础请求地址
                                    String chatModel, // 字段：对话模型名称/端点 ID
                                    String embeddingModel, // 字段：向量化模型名称/端点 ID
                                    boolean webSearchEnabled, // 字段：是否启用联网搜索功能
                                    boolean apiKeyConfigured, // 字段：系统中是否已配置 API 秘钥
                                    String apiKeyMasked, // 字段：已脱敏显示的 API 秘钥（如：sk-****xxxx）
                                    long version, // 字段：配置版本号（用于乐观锁校验）
                                    OffsetDateTime updatedAt) { // 字段：最后一次更新的时间
}