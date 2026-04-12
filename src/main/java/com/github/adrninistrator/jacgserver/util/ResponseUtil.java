package com.github.adrninistrator.jacgserver.util;

import com.github.adrninistrator.jacgserver.constant.ErrorCode;
import com.github.adrninistrator.jacgserver.model.vo.ResponseResult;

/**
 * 响应工具类
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public final class ResponseUtil {

    private ResponseUtil() {
    }

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @return 响应结果
     */
    public static ResponseResult success(Object data) {
        return new ResponseResult(ErrorCode.SUCCESS, "success", data);
    }

    /**
     * 成功响应（无数据）
     *
     * @return 响应结果
     */
    public static ResponseResult success() {
        return new ResponseResult(ErrorCode.SUCCESS, "success", null);
    }

    /**
     * 成功响应（自定义消息）
     *
     * @param message 响应消息
     * @param data    响应数据
     * @return 响应结果
     */
    public static ResponseResult success(String message, Object data) {
        return new ResponseResult(ErrorCode.SUCCESS, message, data);
    }

    /**
     * 错误响应
     *
     * @param code    错误码
     * @param message 错误消息
     * @return 响应结果
     */
    public static ResponseResult error(int code, String message) {
        return new ResponseResult(code, message, null);
    }

    /**
     * 错误响应（带数据）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param data    响应数据
     * @return 响应结果
     */
    public static ResponseResult error(int code, String message, Object data) {
        return new ResponseResult(code, message, data);
    }
}
