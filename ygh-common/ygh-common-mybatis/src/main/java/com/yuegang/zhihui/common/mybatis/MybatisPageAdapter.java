package com.yuegang.zhihui.common.mybatis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuegang.zhihui.common.core.PageRequest;
import com.yuegang.zhihui.common.core.PageResponse;

import java.util.Objects;
import java.util.function.Function;

/**
 * 在持久层分页类型与稳定的外部页面契约之间进行转换。
 */
public class MybatisPageAdapter { // 解耦 MyBatis Plus 与业务 API
    private MybatisPageAdapter() {
    }// 静态工具类，禁止实例化

    public static <T> Page<T> toPage(PageRequest request) { // 将自定义 PageRequest 转为 Mp 的 Page 对象
        Objects.requireNonNull(request, "request must not be null"); // 判断非空
        return new Page<>(request.pageNo(), request.pageSize(), true); // 设置当前页、大小，并且开启总数同居

    }

    /**
     * 将 MyBatis Plus 的 Page 对象转换为自定义的 PageResponse 对象。
     */
    public static <S, T> PageResponse<T> toPageResponse(IPage<S> page, Function<? super S, T> mapper) {
        Objects.requireNonNull(page, "page must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        Objects.requireNonNull(page.getRecords(), "page records must not be null");

        int pageNo = Math.toIntExact(page.getCurrent()); // 转换页码
        int pageSize = Math.toIntExact(page.getSize()); // 转换每页大小
        var request = new PageRequest(pageNo, pageSize); // 创建自定义分页请求对象
        var records = page.getRecords().stream().map(mapper).toList(); // 转换传入的 mapper 转换数据（如 Entity -> DTO）
        return PageResponse.of(records, request, page.getTotal());
    }
}