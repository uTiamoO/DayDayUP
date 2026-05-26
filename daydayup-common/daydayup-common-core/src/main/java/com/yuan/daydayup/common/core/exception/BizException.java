package com.yuan.daydayup.common.core.exception;

import com.yuan.daydayup.common.core.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * <p>业务流程中可预期的、需要向调用方明确反馈的异常类型。
 * 由全局异常处理器捕获后封装为统一返回结构。</p>
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
