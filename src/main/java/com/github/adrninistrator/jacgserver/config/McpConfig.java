package com.github.adrninistrator.jacgserver.config;

import org.springframework.context.annotation.Configuration;

/**
 * MCP (Model Context Protocol) 配置类
 *
 * 当前项目作为 MCP Server，同时支持两种传输协议：
 * 1. HTTP+SSE 传输：SSE 端点 /mcp/sse（GET）+ 消息端点 /mcp/message（POST）
 * 2. Streamable HTTP 传输：统一端点 /mcp/sse（POST），响应直接在 HTTP 响应中返回
 *
 * 协议版本：2024-11-05
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Configuration
public class McpConfig {

    /**
     * MCP SSE 端点路径
     */
    public static final String SSE_ENDPOINT = "/mcp/sse";

    /**
     * MCP 消息接收端点路径
     */
    public static final String MESSAGE_ENDPOINT = "/mcp/message";

    /**
     * MCP 协议版本
     */
    public static final String PROTOCOL_VERSION = "2024-11-05";

    /**
     * MCP Server 名称
     */
    public static final String SERVER_NAME = "java-all-call-graph-server";

    /**
     * MCP Server 版本
     */
    public static final String SERVER_VERSION = "0.0.1";
}
