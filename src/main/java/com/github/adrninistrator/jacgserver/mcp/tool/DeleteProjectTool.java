package com.github.adrninistrator.jacgserver.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.exception.BaseException;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 删除项目的 MCP Tool
 *
 * 仅允许删除通过MCP创建的项目，用于应对MCP创建错项目的场景。
 * 通过前端页面的HTTP请求删除项目时没有以上限制。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class DeleteProjectTool implements McpToolHandler {

    @Autowired
    private ProjectService projectService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.DELETE_PROJECT,
                "删除项目。仅允许删除通过MCP创建的项目，用于应对MCP创建错项目的场景。" +
                "通过前端页面的HTTP请求删除项目时没有以上限制。")
                .addProperty("project_id", "string", "项目ID", true);
    }

    @Override
    public JsonNode handle(JsonNode arguments) {
        String projectId = arguments.has("project_id") ? arguments.get("project_id").asText() : null;

        if (projectId == null || projectId.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("项目ID不能为空");
        }

        try {
            projectService.deleteProjectByMcp(projectId.trim());

            ObjectNode data = objectMapper.createObjectNode();
            data.put("projectId", projectId.trim());
            data.put("deleted", true);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.set("data", data);

            return McpToolHelper.createJsonResult(response);
        } catch (BaseException e) {
            return McpToolHelper.createOperationFailResult(e.getMessage());
        } catch (Exception e) {
            return McpToolHelper.createErrorResult(e.getMessage());
        }
    }
}
