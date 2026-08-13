package com.yuegang.zhihui.ai.domain;

import com.yuegang.zhihui.search.api.SearchHit;

import java.util.List;
import java.util.Set;

/**
 * 知识库检索接口（RAG 架构中的检索环节）
 */
public interface RetrievalGateway {
    List<SearchHit> search(String query, String category, int limit);

    default List<SearchHit> search(String query, String category, int limit, Set<String> visibilities) {
        return search(query, category, limit);
    }
}
