package com.github.adrninistrator.jacgserver.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.constant.Constants;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.exception.BaseException;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.model.vo.ExecutionVO;
import com.github.adrninistrator.jacgserver.service.ProjectService;
import com.github.adrninistrator.jacgserver.service.impl.ExecuteServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 执行默认模板的 MCP Tool（异步执行）
 *
 * AI 可通过此工具执行指定项目的默认模板（向上或向下），传入入口类/方法列表，
 * 返回执行ID，需要调用 query_call_graph_status 查询执行结果。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class ExecuteDefaultTemplateTool implements McpToolHandler {

    @Autowired
    private ExecuteServiceImpl executeServiceImpl;

    @Autowired
    private ProjectService projectService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.EXECUTE_DEFAULT_TEMPLATE,
                "执行默认模板（异步执行）。通过direction参数指定方向：callee（向上调用链）或 caller（向下调用链）。" +
                "需要指定项目ID、方向和入口类/方法列表，" +
                "使用项目的对应默认模板配置执行调用链生成，返回执行ID，" +
                "需要调用 query_call_graph_status 查询执行结果，包含生成调用链的方法与文件完整路径的Map。" +
                "如果默认模板不存在，返回错误信息。")
                .addProperty("project_id", "string", "项目ID", true)
                .addProperty("direction", "string", "调用链方向：callee（向上调用链，被调用方视角，查找谁调用了指定方法）或 caller（向下调用链，调用方视角，查找指定方法调用了哪些方法）", true)
                .addArrayProperty("entry_methods", "string", "入口类/方法列表，支持以下格式：类名（如: com.example.Controller），解析该类所有方法；" +
                        "完整方法（如: com.example.Service:methodName）；" +
                        "带参数的方法（如: com.example.Service:methodName(java.lang.String)）；" +
                        "带参数和返回类型的方法（如: com.example.Service:methodName(java.lang.String):java.lang.String）；" +
                        "代码行号（如: com.example.Service:123）", true);
    }

    @Override
    public JsonNode handle(JsonNode arguments) {
        String projectId = arguments.has("project_id") ? arguments.get("project_id").asText() : null;
        if (projectId == null || projectId.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("项目ID不能为空");
        }

        String direction = arguments.has("direction") ? arguments.get("direction").asText() : null;
        if (direction == null || direction.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("direction参数不能为空，需要指定callee（向上调用链）或caller（向下调用链）");
        }
        if (!Constants.DIRECTION_CALLEE.equals(direction) && !Constants.DIRECTION_CALLER.equals(direction)) {
            return McpToolHelper.createErrorResult("direction参数值无效，需要指定callee（向上调用链）或caller（向下调用链）");
        }

        // 检查项目是否存在
        if (!projectService.projectExists(projectId)) {
            return McpToolHelper.createOperationFailResult("项目不存在: " + projectId);
        }

        JsonNode entryMethodsNode = arguments.get("entry_methods");
        if (entryMethodsNode != null && !entryMethodsNode.isArray()) {
            return McpToolHelper.createErrorResult("参数entry_methods应为数组类型");
        }
        if (entryMethodsNode == null || entryMethodsNode.size() == 0) {
            return McpToolHelper.createErrorResult("入口类/方法列表不能为空");
        }

        List<String> entryMethods = new ArrayList<>();
        for (JsonNode node : entryMethodsNode) {
            String method = node.asText();
            if (method != null && !method.trim().isEmpty()) {
                entryMethods.add(method.trim());
            }
        }

        if (entryMethods.isEmpty()) {
            return McpToolHelper.createErrorResult("入口类/方法列表不能为空");
        }

        try {
            ExecutionVO execution = executeServiceImpl.executeDefaultTemplate(
                    projectId, direction, entryMethods);

            ObjectNode data = objectMapper.createObjectNode();
            data.put("execId", execution.getExecId());
            data.put("projectId", execution.getProjectId());
            data.put("type", execution.getType());
            data.put("status", execution.getStatus());
            if (execution.getStartTime() != null) {
                data.put("startTime", execution.getStartTime());
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
