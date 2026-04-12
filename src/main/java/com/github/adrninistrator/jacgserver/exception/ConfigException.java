package com.github.adrninistrator.jacgserver.exception;

import com.github.adrninistrator.jacgserver.constant.ErrorCode;

/**
 * 配置异常
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ConfigException extends BaseException {

    private static final long serialVersionUID = 1L;

    public ConfigException(String message) {
        super(ErrorCode.CONFIG_ERROR, message);
    }

    public ConfigException(String message, Throwable cause) {
        super(ErrorCode.CONFIG_ERROR, message, cause);
    }
}
