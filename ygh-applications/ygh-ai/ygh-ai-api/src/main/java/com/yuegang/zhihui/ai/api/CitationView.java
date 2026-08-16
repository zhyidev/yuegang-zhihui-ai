package com.yuegang.zhihui.ai.api;

import java.time.OffsetDateTime;

/**
 * AI 回答所参考的知识库原始文档信息
 */
public record CitationView(
        String sourceType, // 来源类型（如：DOC，WEBPAGE）
        String sourceId, // 来源唯一标识
        String documentId, // 文档 ID
        String title, // 文档标题
        String excerpt, // 文档摘要或引用的片段
        String url, // 文档的原始链接
        long documentVersion, // 引用的文档版本号
        OffsetDateTime sourceUpdatedAt // 来源文档的最后更新时间
        ) {
    // 辅助构造函数：用于简化初始化，部分字段可默认为空
    public CitationView(
            String sourceType, String sourceId, String title, String excerpt, String url) {
        this(sourceType, sourceId, null, title, excerpt, url, 0, null);
    }
}
