package com.github.adrninistrator.jacgserver.exception;

import com.github.adrninistrator.jacgserver.constant.ErrorCode;

/**
 * 执行异常
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ExecuteException extends BaseException {

    private static final long serialVersionUID = 1L;

    public ExecuteException(String message) {
        super(ErrorCode.EXECUTE_ERROR, message);
    }

    public ExecuteException(String message, Throwable cause) {
        super(ErrorCode.EXECUTE_ERROR, message, cause);
    }
}
