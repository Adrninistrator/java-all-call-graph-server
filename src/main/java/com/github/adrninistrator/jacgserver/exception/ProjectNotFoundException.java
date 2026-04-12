package com.github.adrninistrator.jacgserver.exception;

import com.github.adrninistrator.jacgserver.constant.ErrorCode;

/**
 * 项目不存在异常
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ProjectNotFoundException extends BaseException {

    private static final long serialVersionUID = 1L;

    public ProjectNotFoundException(String message) {
        super(ErrorCode.PROJECT_NOT_FOUND, message);
    }
}
