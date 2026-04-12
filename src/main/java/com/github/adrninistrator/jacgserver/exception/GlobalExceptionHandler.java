package com.github.adrninistrator.jacgserver.exception;

import com.github.adrninistrator.jacgserver.constant.ErrorCode;
import com.github.adrninistrator.jacgserver.model.vo.ElConfigCheckErrorVO;
import com.github.adrninistrator.jacgserver.model.vo.ResponseResult;
import com.github.adrninistrator.jacgserver.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ElConfigCheckException.class)
    @ResponseBody
    public ResponseResult handleElConfigCheckException(ElConfigCheckException e, HttpServletRequest request) {
        logger.error("请求类型: {}, URI: {}, 表达式配置检查失败: {}", 
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        
        // 构建详细的错误信息对象
        ElConfigCheckErrorVO errorVO = new ElConfigCheckErrorVO(
                e.getConfigSource(),
                e.getElConfigEnumName(),
                e.getConfigFileName(),
                e.getConfigDescription(),
                e.getElText(),
                e.getErrorMessage()
        );
        
        return ResponseUtil.error(ErrorCode.BAD_REQUEST, e.getMessage(), errorVO);
    }

    @ExceptionHandler(BaseException.class)
    @ResponseBody
    public ResponseResult handleBaseException(BaseException e, HttpServletRequest request) {
        logger.error("请求类型: {}, URI: {}, 业务异常: code={}, message={}", 
                request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage(), e);
        return ResponseUtil.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseResult handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        logger.error("请求类型: {}, URI: {}, 参数错误: {}", 
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        return ResponseUtil.error(ErrorCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseResult handleException(Exception e, HttpServletRequest request) {
        logger.error("请求类型: {}, URI: {}, 服务器内部错误: {}", 
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        return ResponseUtil.error(ErrorCode.INTERNAL_ERROR, "服务器内部错误");
    }
}
