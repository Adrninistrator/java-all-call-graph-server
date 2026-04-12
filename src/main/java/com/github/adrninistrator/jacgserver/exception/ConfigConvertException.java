package com.github.adrninistrator.jacgserver.exception;

import com.github.adrninistrator.jacgserver.constant.ErrorCode;

/**
 * 配置参数转换异常
 * 当前端参数转换为JavaCG2ConfigureWrapper/ConfigureWrapper对象过程中出现异常时抛出
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ConfigConvertException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造函数
     *
     * @param message 异常信息
     */
    public ConfigConvertException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }

    /**
     * 构造函数
     *
     * @param message 异常信息
     * @param cause   原始异常
     */
    public ConfigConvertException(String message, Throwable cause) {
        super(ErrorCode.BAD_REQUEST, message, cause);
    }
}
