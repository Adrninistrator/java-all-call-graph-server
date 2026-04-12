package com.github.adrninistrator.jacgserver.constant;

/**
 * 错误码定义
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public final class ErrorCode {

    private ErrorCode() {
    }

    /**
     * 成功
     */
    public static final int SUCCESS = 200;

    /**
     * 请求参数错误
     */
    public static final int BAD_REQUEST = 400;

    /**
     * 资源不存在
     */
    public static final int NOT_FOUND = 404;

    /**
     * 服务器内部错误
     */
    public static final int INTERNAL_ERROR = 500;

    /**
     * 项目不存在
     */
    public static final int PROJECT_NOT_FOUND = 1001;

    /**
     * 模板不存在
     */
    public static final int TEMPLATE_NOT_FOUND = 1002;

    /**
     * 配置文件读写错误
     */
    public static final int CONFIG_ERROR = 1003;

    /**
     * 执行失败
     */
    public static final int EXECUTE_ERROR = 1004;
}
