package com.tanglinlin.picture.backend.common;

import lombok.Data;

/**
 * @Author:TSL
 * @Date: 2025/8/30  19:44
 * @Description
 */
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}

