package com.github.adrninistrator.jacgserver.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.model.vo.ExecutionVO;
import com.github.adrninistrator.jacgserver.service.ExecuteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 查询特定项目某次执行静态分析状态的 MCP Tool
 *
 * AI 可通过此工具查询指定项目是否正在执行静态分析，以及执行状态和耗时等信息。
 * 该接口同时支持HTTP接口和MCP服务接口调用。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class QueryAnalysisStatusTool implements McpToolHandler {

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private com.github.adrninistrator.jacgserver.service.ProjectService projectService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.QUERY_ANALYSIS_STATUS,
                "查询特定项目是否正在执行静态分析，以及执行状态和耗时等信息。")
                .addProperty("project_id", "string", "项目ID", true);
    }

    @Override
    public JsonNode handle(JsonNode arguments) {
        String projectId = arguments.has("project_id") ? arguments.get("project_id").asText() : null;
        if (projectId == null || projectId.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("项目ID不能为空");
        }

        try {
            // 检查项目是否存在
            if (!projectService.projectExists(projectId)) {
                return McpToolHelper.createQueryFailResult("项目不存在: " + projectId);
            }

            boolean executing = executeService.isProjectExecuting(projectId);

            ObjectNode data = objectMapper.createObjectNode();
            data.put("executing", executing);

            ExecutionVO executionInfo = executeService.getProjectExecutionInfo(projectId);
            if (executionInfo != null) {
                if (executionInfo.getStatus() != null) {
                    data.put("status", executionInfo.getStatus());
                }
                if (executionInfo.getDuration() != null) {
                    data.put("duration", executionInfo.getDuration());
                }
                if (executionInfo.getExecId() != null) {
                    data.put("execId", executionInfo.getExecId());
                }
                if (executionInfo.getErrorMessage() != null) {
                    data.put("errorMessage", executionInfo.getErrorMessage());
                }
            }

            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.set("data", data);

            return McpToolHelper.createJsonResult(response);
        } catch (Exception e) {
            return McpToolHelper.createErrorResult(e.getMessage());
        }
    }
}
