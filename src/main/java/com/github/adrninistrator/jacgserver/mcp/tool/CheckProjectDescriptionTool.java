package com.github.adrninistrator.jacgserver.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 判断项目描述是否存在的 MCP Tool
 *
 * AI 可通过此工具判断项目描述是否已存在，避免创建项目时因描述重复而失败。
 * 该接口仅提供MCP服务方式调用，不提供HTTP接口。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class CheckProjectDescriptionTool implements McpToolHandler {

    @Autowired
    private ProjectService projectService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.CHECK_PROJECT_DESCRIPTION,
                "判断项目描述是否已存在。返回项目描述是否已存在的标志，可用于创建项目前判断描述是否可用。" +
                "项目描述在全局范围内唯一。")
                .addProperty("description", "string", "项目描述", true);
    }

    @Override
    public JsonNode handle(JsonNode arguments) {
        String description = arguments.has("description") ? arguments.get("description").asText() : null;

        if (description == null || description.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("项目描述不能为空");
        }

        boolean exists = projectService.isProjectDescriptionExists(description.trim());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("description", description.trim());
        data.put("exists", exists);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("success", true);
        response.set("data", data);

        return McpToolHelper.createJsonResult(response);
    }
}
