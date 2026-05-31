package com.github.adrninistrator.jacgserver.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * MCP Tool 处理器接口
 *
 * 所有 MCP Tool 的处理器都需要实现此接口。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public interface McpToolHandler {

    /**
     * 获取工具定义
     *
     * @return 工具定义
     */
    McpToolDefinition getDefinition();

    /**
     * 处理工具调用
     *
     * @param arguments 调用参数
     * @return 调用结果，格式为 MCP tool result
     */
    JsonNode handle(JsonNode arguments);
}
