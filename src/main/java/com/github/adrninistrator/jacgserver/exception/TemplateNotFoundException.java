package com.github.adrninistrator.jacgserver.exception;

import com.github.adrninistrator.jacgserver.constant.ErrorCode;

/**
 * 模板不存在异常
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class TemplateNotFoundException extends BaseException {

    private static final long serialVersionUID = 1L;

    public TemplateNotFoundException(String message) {
        super(ErrorCode.TEMPLATE_NOT_FOUND, message);
    }
}
