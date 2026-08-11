package com.yuegang.zhihui.ai.domain;

/**
 * 联网工具返回的公开来源，不承载内部文档权限事实。
 */
public record ModelSource(
    String title, // 来源网页或文档的标题
    String excerpt, // 来源内容的精彩片段或摘要
    String url // 来源的原始链接地址
) {
}
