package com.github.adrninistrator.jacgserver.exception;

/**
 * 基础异常类
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BaseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
