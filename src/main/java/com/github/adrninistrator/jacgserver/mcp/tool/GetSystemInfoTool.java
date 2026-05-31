package com.github.adrninistrator.jacgserver.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.service.ConfigService;
import com.github.adrninistrator.jacgserver.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 获取系统信息 MCP Tool
 *
 * 测试用的 MCP Tool，返回系统基本信息，包括输出根目录、应用根目录等。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class GetSystemInfoTool implements McpToolHandler {

    @Autowired
    private ConfigService configService;

    @Autowired
    private SystemService systemService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.GET_SYSTEM_INFO,
                "获取 java-all-call-graph-server 系统信息。默认返回应用名称和MCP协议版本；当 include_path 为 true 时，额外返回输出根目录和应用根目录")
                .addProperty("include_path", "boolean", "是否包含目录路径信息（输出根目录、应用根目录），默认false", false);
    }

    @Override
    public JsonNode handle(JsonNode arguments) {
        boolean includePath = arguments.has("include_path") && arguments.get("include_path").asBoolean(false);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("appName", "java-all-call-graph-server");
        data.put("mcpProtocolVersion", "2024-11-05");

        if (includePath) {
            data.put("outputRootPath", configService.getOutputRootPath());
            data.put("appRootPath", systemService.getAppRootPath());
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", true);
        result.set("data", data);

        return McpToolHelper.createJsonResult(result);
    }
}
