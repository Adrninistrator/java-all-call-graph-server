package com.github.adrninistrator.jacgserver.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.config.McpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 请求处理服务
 *
 * 处理 MCP JSON-RPC 请求，包括初始化、工具列表查询和工具调用。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Service
public class McpRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(McpRequestHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 已注册的 MCP Tool 定义
     */
    private final Map<String, McpToolDefinition> toolDefinitions = new LinkedHashMap<>();

    /**
     * 已注册的 MCP Tool 处理器
     */
    private final Map<String, McpToolHandler> toolHandlers = new HashMap<>();

    /**
     * SSE 会话管理：sessionId -> McpSseSession
     */
    private final Map<String, McpSseSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private List<McpToolHandler> autoWiredHandlers;

    @PostConstruct
    public void init() {
        // 自动注册所有 McpToolHandler 实现
        if (autoWiredHandlers != null) {
            for (McpToolHandler handler : autoWiredHandlers) {
                registerTool(handler);
            }
        }
        logger.info("MCP Server 初始化完成，已注册 {} 个工具", toolDefinitions.size());
    }

    /**
     * 注册 MCP Tool
     */
    public void registerTool(McpToolHandler handler) {
        McpToolDefinition definition = handler.getDefinition();
        toolDefinitions.put(definition.getName(), definition);
        toolHandlers.put(definition.getName(), handler);
        logger.info("注册 MCP Tool: {}", definition.getName());
    }

    /**
     * 创建新的 SSE 会话
     */
    public McpSseSession createSession() {
        String sessionId = UUID.randomUUID().toString();
        McpSseSession session = new McpSseSession(sessionId);

        // 设置会话关闭回调，超时或异常时自动清理会话
        session.setOnSessionClose(() -> removeSession(sessionId));

        sessions.put(sessionId, session);
        logger.info("创建 MCP SSE 会话: {}", sessionId);
        return session;
    }

    /**
     * 获取会话
     */
    public McpSseSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 移除会话
     */
    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
        logger.info("移除 MCP SSE 会话: {}", sessionId);
    }

    /**
     * 处理 JSON-RPC 请求（Streamable HTTP 传输方式，需要关联会话）
     *
     * @param request JSON-RPC 请求
     * @param session 当前会话
     * @return JSON-RPC 响应，通知类型返回 null
     */
    public JsonNode handleRequest(JsonNode request, McpSseSession session) {
        String method = request.has("method") ? request.get("method").asText() : null;
        JsonNode id = request.get("id");
        JsonNode params = request.get("params");

        logger.debug("处理 MCP 请求: method={}, id={}", method, id);

        try {
            JsonNode result;

            switch (method) {
                case "initialize":
                    result = handleInitialize(params, session);
                    break;
                case "notifications/initialized":
                    // 客户端初始化完成通知，标记会话为已初始化
                    if (session != null) {
                        session.setInitialized(true);
                        logger.info("MCP 会话已初始化: sessionId={}", session.getSessionId());
                    }
                    return null;
                case "tools/list":
                    result = handleToolsList();
                    break;
                case "tools/call":
                    result = handleToolsCall(params);
                    break;
                case "ping":
                    result = objectMapper.createObjectNode();
                    break;
                default:
                    return createErrorResponse(id, -32601, "Method not found: " + method);
            }

            return createSuccessResponse(id, result);
        } catch (Exception e) {
            logger.error("处理 MCP 请求异常: method={}", method, e);
            return createErrorResponse(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * 处理 JSON-RPC 请求（HTTP+SSE 传输方式，不需要关联会话）
     */
    public JsonNode handleRequest(JsonNode request) {
        return handleRequest(request, null);
    }

    /**
     * 处理 initialize 请求
     */
    private ObjectNode handleInitialize(JsonNode params, McpSseSession session) {
        // 检查客户端协议版本兼容性
        if (params != null && params.has("protocolVersion")) {
            String clientProtocolVersion = params.get("protocolVersion").asText();
            if (!McpConfig.PROTOCOL_VERSION.equals(clientProtocolVersion)) {
                logger.warn("客户端协议版本不匹配: client={}, server={}", clientProtocolVersion, McpConfig.PROTOCOL_VERSION);
            }
        }

        // 保存客户端能力信息
        if (session != null && params != null && params.has("capabilities")) {
            session.setClientCapabilities(params.get("capabilities"));
            logger.debug("已保存客户端能力信息: sessionId={}", session.getSessionId());
        }

        ObjectNode result = objectMapper.createObjectNode();

        ObjectNode serverInfo = objectMapper.createObjectNode();
        serverInfo.put("name", McpConfig.SERVER_NAME);
        serverInfo.put("version", McpConfig.SERVER_VERSION);
        result.set("serverInfo", serverInfo);
        result.put("protocolVersion", McpConfig.PROTOCOL_VERSION);

        ObjectNode capabilities = objectMapper.createObjectNode();
        capabilities.set("tools", objectMapper.createObjectNode());
        result.set("capabilities", capabilities);

        return result;
    }

    /**
     * 处理 tools/list 请求
     */
    private ObjectNode handleToolsList() {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode tools = objectMapper.createArrayNode();

        for (McpToolDefinition definition : toolDefinitions.values()) {
            ObjectNode tool = objectMapper.createObjectNode();
            tool.put("name", definition.getName());
            tool.put("description", definition.getDescription());
            tool.set("inputSchema", objectMapper.valueToTree(definition.toJsonSchema()));
            tools.add(tool);
        }

        result.set("tools", tools);
        return result;
    }

    /**
     * 处理 tools/call 请求
     */
    private JsonNode handleToolsCall(JsonNode params) {
        String toolName = params.has("name") ? params.get("name").asText() : null;
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : objectMapper.createObjectNode();

        if (toolName == null || toolName.isEmpty()) {
            ObjectNode errorResult = objectMapper.createObjectNode();
            errorResult.put("isError", true);
            ArrayNode content = objectMapper.createArrayNode();
            ObjectNode textContent = objectMapper.createObjectNode();
            textContent.put("type", "text");
            textContent.put("text", "Missing tool name");
            content.add(textContent);
            errorResult.set("content", content);
            return errorResult;
        }

        McpToolHandler handler = toolHandlers.get(toolName);
        if (handler == null) {
            ObjectNode errorResult = objectMapper.createObjectNode();
            errorResult.put("isError", true);
            ArrayNode content = objectMapper.createArrayNode();
            ObjectNode textContent = objectMapper.createObjectNode();
            textContent.put("type", "text");
            textContent.put("text", "Unknown tool: " + toolName);
            content.add(textContent);
            errorResult.set("content", content);
            return errorResult;
        }

        try {
            return handler.handle(arguments);
        } catch (Exception e) {
            logger.error("执行 MCP Tool 异常: tool={}", toolName, e);
            return McpToolHelper.createErrorResult(e.getMessage());
        }
    }

    /**
     * 创建成功响应
     */
    private ObjectNode createSuccessResponse(JsonNode id, JsonNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.set("id", id);
        }
        response.set("result", result);
        return response;
    }

    /**
     * 创建错误响应
     */
    private ObjectNode createErrorResponse(JsonNode id, int code, String message) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.set("id", id);
        }
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        response.set("error", error);
        return response;
    }
}
