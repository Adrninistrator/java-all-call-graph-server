package com.github.adrninistrator.jacgserver.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.exception.BaseException;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.model.vo.ExecutionVO;
import com.github.adrninistrator.jacgserver.service.ExecuteService;
import com.github.adrninistrator.jacgserver.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 执行静态分析的 MCP Tool
 *
 * AI 可通过此工具对指定项目执行静态分析。
 * 该接口同时支持HTTP接口和MCP服务接口调用。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class ExecuteAnalysisTool implements McpToolHandler {

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private ProjectService projectService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.EXECUTE_ANALYSIS,
                "对指定项目执行静态分析。需要指定项目ID，返回执行ID和执行状态等信息。")
                .addProperty("project_id", "string", "项目ID", true);
    }

    @Override
    public JsonNode handle(JsonNode arguments) {
        String projectId = arguments.has("project_id") ? arguments.get("project_id").asText() : null;
        if (projectId == null || projectId.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("项目ID不能为空");
        }

        // 检查项目是否存在
        if (!projectService.projectExists(projectId)) {
            return McpToolHelper.createOperationFailResult("项目不存在: " + projectId);
        }

        try {
            ExecutionVO execution = executeService.executeAnalysis(projectId);

            ObjectNode data = objectMapper.createObjectNode();
            data.put("execId", execution.getExecId());
            data.put("projectId", execution.getProjectId());
            data.put("type", execution.getType());
            data.put("status", execution.getStatus());
            if (execution.getStartTime() != null) {
                data.put("startTime", execution.getStartTime());
            }
            if (execution.getEndTime() != null) {
                data.put("endTime", execution.getEndTime());
            }
            if (execution.getOutputDir() != null) {
                data.put("outputDir", execution.getOutputDir());
            }
            if (execution.getErrorMessage() != null) {
                data.put("errorMessage", execution.getErrorMessage());
            }
            if (execution.getDuration() != null) {
                data.put("duration", execution.getDuration());
            }
            if (execution.getLogFilePath() != null) {
                data.put("logFilePath", execution.getLogFilePath());
            }

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
