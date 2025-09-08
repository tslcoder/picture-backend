package com.tanglinlin.picture.backend.common;

import com.tanglinlin.picture.backend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author:TSL
 * @Date: 2025/8/30  19:43
 * @Description
 */
@Data
public class BaseResponse<T> implements Serializable {
    private int code;
    private T data;
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }

}
