package com.tanglinlin.picture.backend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author:TSL
 * @Date: 2025/8/30  19:44
 * @Description
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}

