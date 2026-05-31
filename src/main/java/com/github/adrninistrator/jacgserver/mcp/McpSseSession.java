package com.github.adrninistrator.jacgserver.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * MCP SSE 会话
 *
 * 封装 SSE 连接和会话信息。
 * SSE连接超时属于正常现象，客户端会自动重连。
 * 支持通过 Mcp-Session-Id 头重连已有会话，重新创建 SseEmitter。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class McpSseSession {

    private static final Logger logger = LoggerFactory.getLogger(McpSseSession.class);

    /** SSE连接超时时间（毫秒），5分钟 */
    public static final long SSE_TIMEOUT = 300000L;

    private final String sessionId;
    private volatile SseEmitter emitter;
    private volatile boolean initialized;

    /** 客户端能力信息 */
    private volatile JsonNode clientCapabilities;

    /** 超时或完成时的回调，用于清理会话 */
    private Runnable onSessionClose;

    public McpSseSession(String sessionId) {
        this.sessionId = sessionId;
        this.emitter = createEmitter();
        this.initialized = false;
    }

    /**
     * 创建新的 SseEmitter 并注册回调
     */
    private SseEmitter createEmitter() {
        SseEmitter newEmitter = new SseEmitter(SSE_TIMEOUT);

        newEmitter.onCompletion(() -> {
            logger.info("MCP SSE 连接完成: sessionId={}", sessionId);
            if (onSessionClose != null) {
                onSessionClose.run();
            }
        });

        newEmitter.onTimeout(() -> {
            logger.info("MCP SSE 连接超时: sessionId={}，客户端会自动重连", sessionId);
            if (onSessionClose != null) {
                onSessionClose.run();
            }
        });

        newEmitter.onError(e -> {
            logger.warn("MCP SSE 连接异常: sessionId={}, error={}", sessionId, e.getMessage());
            if (onSessionClose != null) {
                onSessionClose.run();
            }
        });

        return newEmitter;
    }

    /**
     * 刷新 SseEmitter，用于 SSE 重连
     * 客户端通过 Mcp-Session-Id 头重连时调用，创建新的 SseEmitter 替代已超时的旧连接
     */
    public void refreshEmitter() {
        try {
            this.emitter.complete();
        } catch (Exception e) {
            // 旧 emitter 可能已超时或关闭，忽略异常
        }
        this.emitter = createEmitter();
        logger.info("MCP SSE 会话已刷新 SseEmitter: sessionId={}", sessionId);
    }

    /**
     * 设置会话关闭时的回调
     *
     * @param onSessionClose 关闭回调
     */
    public void setOnSessionClose(Runnable onSessionClose) {
        this.onSessionClose = onSessionClose;
    }

    /**
     * 发送 SSE 事件
     */
    public void sendEvent(String eventName, String data) throws IOException {
        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name(eventName)
                .data(data);
        emitter.send(event);
    }

    /**
     * 完成SSE连接
     */
    public void complete() {
        emitter.complete();
    }

    public String getSessionId() {
        return sessionId;
    }

    public SseEmitter getEmitter() {
        return emitter;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public JsonNode getClientCapabilities() {
        return clientCapabilities;
    }

    public void setClientCapabilities(JsonNode clientCapabilities) {
        this.clientCapabilities = clientCapabilities;
    }
}
