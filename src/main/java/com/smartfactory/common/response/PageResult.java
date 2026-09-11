package com.smartfactory.common.response;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    /**
     * 当前页数据
     */
    private List<T> records;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页数量
     */
    private Integer size;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Long pages;

    public PageResult(
            List<T> records,
            Integer page,
            Integer size,
            Long total) {

        this.records = records;
        this.page = page;
        this.size = size;
        this.total = total;

        this.pages = (total + size - 1) / size;
    }
}