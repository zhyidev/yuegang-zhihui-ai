package com.yuegang.zhihui.common.core;


import java.util.List;
import java.util.Objects;

/*
*  在{@Link ApiResponse} 内部返回的稳定的分页数据负载
* */
public record PageResponse<T>(
        List<T> records,
        int pageNo,
        int pageSize,
        long total,
        long pages) {

    public PageResponse{ // 构造逻辑
        records = List.copyOf(Objects.requireNonNull(records,"records must not be null")); // 非空不可变列表
        if (pageNo < 1){
            throw new IllegalArgumentException("pageNo must be at least 1");
        }
        if (pageSize < 1 || pageSize > PageRequset.MAX_PAGE_SIZE){
            throw new IllegalArgumentException("pageSize is outside the supported range");
        }
        if (total < 0 || pages < 0){
            throw new IllegalArgumentException("total and pages must not be negative ");
        }
    }

    public <T> PageResponse<T> of(List<T> records, PageRequset requset, long total){ // 静态转换
        Objects.requireNonNull(records, "records must not be null"); // 校验分页数请求非空总页数算法
        var pages =total == 0 ? 0 : ((total - 1) / requset.pageSize()) + 1; // 自动计算总页数算法
        return new PageResponse<>(records, requset.pageNo(), requset.pageSize(), total,pages);
    }

}
