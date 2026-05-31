package com.github.adrninistrator.jacgserver.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.adrninistrator.jacgserver.config.McpConfig;
import com.github.adrninistrator.jacgserver.mcp.McpRequestHandler;
import com.github.adrninistrator.jacgserver.mcp.McpSseSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP (Model Context Protocol) 控制器
 *
 * 同时支持两种 MCP 传输协议：
 *
 * 1. HTTP+SSE 传输（兼容旧版客户端）：
 *    - GET  /mcp/sse       建立 SSE 连接，客户端通过此连接接收服务端消息
 *    - POST /mcp/message   客户端通过此端点发送 JSON-RPC 请求，响应通过 SSE 连接返回
 *
 * 2. Streamable HTTP 传输（MCP 2025-03-26 版本新增）：
 *    - POST /mcp/sse       客户端发送 JSON-RPC 请求，服务端根据 Accept 头决定响应格式
 *                          - Accept: application/json → 直接返回 JSON 响应
 *                          - Accept: text/event-stream → 返回 SSE 流
 *    - GET  /mcp/sse       建立 SSE 连接，支持通过 Mcp-Session-Id 头重连已有会话
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@RestController
@RequestMapping
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    /**
     * Streamable HTTP 传输的会话ID响应头名称
     */
    private static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";

    @Autowired
    private McpRequestHandler requestHandler;

    /**
     * SSE 端点 - 建立 SSE 连接
     *
     * 支持两种场景：
     * 1. 新建连接：客户端不带 Mcp-Session-Id 头，服务端创建新会话
     * 2. 重连：客户端携带 Mcp-Session-Id 头，服务端恢复已有会话并刷新 SseEmitter
     */
    @GetMapping(value = McpConfig.SSE_ENDPOINT, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSse(
            HttpServletRequest request,
            @RequestHeader(value = MCP_SESSION_ID_HEADER, required = false) String sessionHeader) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        McpSseSession session;

        // 支持通过 Mcp-Session-Id 头重连已有会话
        if (sessionHeader != null && !sessionHeader.isEmpty()) {
            session = requestHandler.getSession(sessionHeader);
            if (session != null) {
                session.refreshEmitter();
                logger.info("MCP SSE 重连: sessionId={}", sessionHeader);
            } else {
                logger.warn("MCP SSE 重连失败，会话不存在: sessionId={}，创建新会话", sessionHeader);
                session = requestHandler.createSession();
            }
        } else {
            session = requestHandler.createSession();
        }

        final McpSseSession finalSession = session;
        SseEmitter emitter = finalSession.getEmitter();

        logger.info("收到 MCP SSE 连接请求，服务端口: {}，SSE 端点: {}://{}:{}{}{}，sessionId={}",
                serverPort, scheme, serverName, serverPort, contextPath, McpConfig.SSE_ENDPOINT, finalSession.getSessionId());

        // 构建消息端点 URL
        StringBuilder endpointUrl = new StringBuilder();
        endpointUrl.append(scheme).append("://").append(serverName);
        if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
            endpointUrl.append(":").append(serverPort);
        }
        endpointUrl.append(contextPath).append(McpConfig.MESSAGE_ENDPOINT)
                .append("?sessionId=").append(session.getSessionId());

        // 异步发送 endpoint 事件
        sseExecutor.execute(() -> {
            try {
                Thread.sleep(100); // 短暂延迟，确保 SSE 连接已建立
                finalSession.sendEvent("endpoint", endpointUrl.toString());
                logger.info("已发送 MCP endpoint 事件: {}", endpointUrl);
            } catch (Exception e) {
                logger.error("发送 MCP endpoint 事件失败", e);
                requestHandler.removeSession(finalSession.getSessionId());
            }
        });

        return emitter;
    }

    /**
     * Streamable HTTP 传输端点 - 接收客户端 JSON-RPC 请求
     *
     * 根据客户端 Accept 请求头决定响应格式：
     * - Accept: application/json → 直接返回 JSON 响应
     * - Accept: text/event-stream → 返回 SSE 流
     * - 其他 → 默认返回 JSON 响应
     *
     * 通过 Mcp-Session-Id 头管理会话：
     * - 首次请求（通常为 initialize）不带 Mcp-Session-Id 头，服务端创建新会话
     * - 后续请求携带 Mcp-Session-Id 头，服务端关联已有会话
     */
    @PostMapping(value = McpConfig.SSE_ENDPOINT)
    public ResponseEntity<?> handleStreamableHttp(
            @RequestBody String body,
            @RequestHeader(value = MCP_SESSION_ID_HEADER, required = false) String sessionHeader,
            @RequestHeader(value = "Accept", required = false) String acceptHeader) {

        // 获取或创建会话
        McpSseSession session;
        if (sessionHeader != null && !sessionHeader.isEmpty()) {
            session = requestHandler.getSession(sessionHeader);
            if (session == null) {
                logger.warn("Streamable HTTP 传输：未找到 MCP 会话: sessionId={}", sessionHeader);
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32001,\"message\":\"Session not found\"}}");
            }
        } else {
            // 首次请求（通常是 initialize），创建新会话
            session = requestHandler.createSession();
            logger.info("Streamable HTTP 传输：创建新会话: sessionId={}", session.getSessionId());
        }

        try {
            JsonNode request = objectMapper.readTree(body);
            // 传入 session，使 handleRequest 能关联会话
            JsonNode response = requestHandler.handleRequest(request, session);

            // 设置会话ID响应头
            HttpHeaders headers = new HttpHeaders();
            headers.set(MCP_SESSION_ID_HEADER, session.getSessionId());

            // 如果是通知类型请求（无id），不需要响应
            if (response == null) {
                return ResponseEntity.accepted().headers(headers).build();
            }

            String responseJson = objectMapper.writeValueAsString(response);

            // 根据 Accept 头决定响应格式
            if (acceptHeader != null && acceptHeader.contains("text/event-stream")) {
                // 返回 SSE 流
                SseEmitter emitter = new SseEmitter(McpSseSession.SSE_TIMEOUT);
                sseExecutor.execute(() -> {
                    try {
                        emitter.send(SseEmitter.event().data(responseJson, MediaType.APPLICATION_JSON));
                        emitter.complete();
                    } catch (Exception e) {
                        logger.error("Streamable HTTP SSE 响应发送失败: sessionId={}", session.getSessionId(), e);
                        emitter.completeWithError(e);
                    }
                });
                return ResponseEntity.ok().headers(headers)
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .body(emitter);
            }

            // 默认返回 JSON
            return ResponseEntity.ok().headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseJson);
        } catch (IOException e) {
            logger.error("Streamable HTTP 传输：处理 MCP 消息异常: sessionId={}", session.getSessionId(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Streamable HTTP 传输：处理 MCP 消息异常: sessionId={}", session.getSessionId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 消息端点 - 接收客户端 JSON-RPC 请求（HTTP+SSE 传输方式）
     *
     * 客户端通过此端点发送 JSON-RPC 请求，服务端处理后将响应通过 SSE 连接返回。
     */
    @PostMapping(value = McpConfig.MESSAGE_ENDPOINT)
    public ResponseEntity<Void> handleMessage(
            @RequestParam("sessionId") String sessionId,
            @RequestBody String body) {

        logger.debug("收到 MCP 消息: sessionId={}", sessionId);

        McpSseSession session = requestHandler.getSession(sessionId);
        if (session == null) {
            logger.warn("未找到 MCP 会话: sessionId={}", sessionId);
            return ResponseEntity.badRequest().build();
        }

        try {
            JsonNode request = objectMapper.readTree(body);
            JsonNode response = requestHandler.handleRequest(request, session);

            // 如果是通知类型请求（无id），不需要响应
            if (response == null) {
                return ResponseEntity.accepted().build();
            }

            // 通过 SSE 连接发送响应
            String responseJson = objectMapper.writeValueAsString(response);
            session.sendEvent("message", responseJson);

            logger.debug("MCP 响应已发送: sessionId={}", sessionId);
            return ResponseEntity.accepted().build();
        } catch (IOException e) {
            logger.error("处理 MCP 消息异常: sessionId={}", sessionId, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("处理 MCP 消息异常: sessionId={}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
