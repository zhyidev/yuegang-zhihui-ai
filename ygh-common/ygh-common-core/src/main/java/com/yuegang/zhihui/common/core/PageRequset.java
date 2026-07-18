package com.yuegang.zhihui.common.core;

/*
*  以1为索引的API分页请求 带有一个强制性上限
* */
public record PageRequset(int pageNo,int pageSize) {
    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public PageRequset{
        if (pageNo < 1){
            throw new IllegalArgumentException("pageNo must be at least 1");
        }
        if (pageSize < 1 ||  pageSize > MAX_PAGE_SIZE){
            throw new IllegalArgumentException("pageSize must be between 1 and " +  MAX_PAGE_SIZE);
        }
    }

    public static PageRequset defualts(){ // 获取默认配置
        return new PageRequset(DEFAULT_PAGE_NO,DEFAULT_PAGE_SIZE);
    }

    public long offset(){ // 计算数据库查询所需偏移量（offset）
        return Math.multiplyExact((long)pageNo - 1L,pageSize);
    }

}
