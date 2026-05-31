package com.github.adrninistrator.jacgserver.exception;

import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import com.github.adrninistrator.jacgserver.config.McpConfig;
import com.github.adrninistrator.jacgserver.constant.ErrorCode;
import com.github.adrninistrator.jacgserver.model.vo.ElConfigCheckErrorVO;
import com.github.adrninistrator.jacgserver.model.vo.ResponseResult;
import com.github.adrninistrator.jacgserver.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

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

    /**
     * 处理异步请求超时异常（如SSE连接超时）
     * SSE连接超时属于正常现象，客户端会自动重连，不需要打印堆栈信息。
     * SSE连接的响应Content-Type已设为text/event-stream，无法写入JSON响应体，因此对SSE请求返回空body。
     * 仅对GET方式的SSE请求做特殊处理，POST方式的Streamable HTTP请求不在此处理。
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<?> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        // 仅对GET方式的/mcp/sse请求做特殊处理（SSE连接超时）
        if (requestURI.startsWith(McpConfig.SSE_ENDPOINT) && "GET".equals(method)) {
            // SSE连接超时，由SseEmitter.onTimeout()回调处理，不需要写入响应体
            logger.info("异步请求超时(SSE), URI: {}", requestURI);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        logger.warn("异步请求超时, URI: {}", requestURI);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseUtil.error(ErrorCode.INTERNAL_ERROR, "异步请求超时"));
    }

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

    @ExceptionHandler(JavaCG2ConfigException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseResult handleJavaCG2ConfigException(JavaCG2ConfigException e, HttpServletRequest request) {
        logger.error("请求类型: {}, URI: {}, 修改项目配置参数失败: {}", 
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        return ResponseUtil.error(ErrorCode.BAD_REQUEST, "修改项目配置参数失败: " + e.getMessage());
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
        return ResponseUtil.error(ErrorCode.INTERNAL_ERROR, e.getClass().getSimpleName() + ": " + e.getMessage());
    }
}
