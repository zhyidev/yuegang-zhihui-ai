package com.yuegang.zhihui.ai.domain;

import java.util.List;

/**
 * 知识库检索接口（RAG 架构中的检索环节）
 */
public interface RetrievalGateway {
    // 基础检索：根据查询词、分类和限制数量获取搜索命中结果
    List<SearchHit>
}