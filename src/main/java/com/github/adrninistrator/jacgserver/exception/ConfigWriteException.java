package com.github.adrninistrator.jacgserver.exception;

import com.github.adrninistrator.jacgserver.constant.ErrorCode;

/**
 * 配置文件写入异常
 * 当genMainConfig、genOtherConfig、genElConfig方法返回false时抛出
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ConfigWriteException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造函数
     *
     * @param message 异常信息
     */
    public ConfigWriteException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }

    /**
     * 构造函数
     *
     * @param message 异常信息
     * @param cause   原始异常
     */
    public ConfigWriteException(String message, Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, message, cause);
    }
}
