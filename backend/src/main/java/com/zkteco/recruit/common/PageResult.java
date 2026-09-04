package com.zkteco.recruit.common;

import java.util.List;
import java.util.function.Function;

import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 分页响应结构（§15.8）：{ list, total, page, size }
 */
public class PageResult<T> {

    private List<T> list;
    private long total;
    private long page;
    private long size;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, long page, long size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <E, T> PageResult<T> of(IPage<E> source, Function<E, T> mapper) {
        List<T> rows = source.getRecords().stream().map(mapper).toList();
        return new PageResult<>(rows, source.getTotal(), source.getCurrent(), source.getSize());
    }

    public static <T> PageResult<T> of(List<T> list, long total, long page, long size) {
        return new PageResult<>(list, total, page, size);
    }

    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(List.of(), 0, page, size);
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
