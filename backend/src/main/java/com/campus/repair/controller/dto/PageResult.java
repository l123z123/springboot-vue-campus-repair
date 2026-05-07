package com.campus.repair.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 分页结果 DTO，确保前端正确解析 records 和 total
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long size;
    private long current;
    private long pages;

    public static <T> PageResult<T> from(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setSize(page.getSize());
        result.setCurrent(page.getCurrent());
        result.setPages(page.getPages());
        return result;
    }
}
