package com.github.adrninistrator.jacgserver.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.constant.Constants;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.model.entity.CallGraphExecutionRecord;
import com.github.adrninistrator.jacgserver.model.entity.CallGraphFileInfo;
import com.github.adrninistrator.jacgserver.model.vo.ExecutionVO;
import com.github.adrninistrator.jacgserver.service.ExecuteService;
import com.github.adrninistrator.jacgserver.service.ProjectService;
import com.github.adrninistrator.jacgserver.service.TemplateExecutionRecordService;
import com.github.adrninistrator.jacgserver.service.impl.ExecuteServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 查询特定项目的默认模板调用链执行状态的 MCP Tool
 *
 * AI 可通过此工具查询指定项目的默认模板（向上或向下）是否正在执行调用链生成，
 * 以及执行状态和耗时等信息。
 * 当传入执行ID时，可查询指定执行的详细结果，包括生成调用链的方法与文件路径Map。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class QueryCallGraphStatusTool implements McpToolHandler {

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private ExecuteServiceImpl executeServiceImpl;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TemplateExecutionRecordService templateExecutionRecordService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.QUERY_CALL_GRAPH_STATUS,
                "查询特定项目的默认模板调用链执行状态。通过direction参数指定方向：callee（向上调用链）或 caller（向下调用链）。" +
                "需要指定项目ID和方向，" +
                "不指定执行ID时返回对应方向默认模板的当前执行状态（是否正在执行、执行状态等），" +
                "指定执行ID时返回该执行的详细结果（包含生成调用链的方法与文件路径Map）。")
                .addProperty("project_id", "string", "项目ID", true)
                .addProperty("direction", "string", "调用链方向：callee（向上调用链）或 caller（向下调用链）", true)
                .addProperty("exec_id", "string", "执行ID（可选），不指定时返回最近一次执行的状态，指定时返回该执行的详细结果（包含调用链文件路径Map）", false);
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

        String execId = arguments.has("exec_id") ? arguments.get("exec_id").asText() : null;

        try {
            // 检查项目是否存在
            if (!projectService.projectExists(projectId)) {
                return McpToolHelper.createQueryFailResult("项目不存在: " + projectId);
            }

            // 如果指定了执行ID，查询指定执行的详细结果
            if (execId != null && !execId.trim().isEmpty()) {
                return queryByExecId(execId, projectId);
            }

            // 未指定执行ID，查询默认模板的当前执行状态
            return queryDefaultTemplateStatus(projectId, direction);
        } catch (Exception e) {
            return McpToolHelper.createErrorResult(e.getMessage());
        }
    }

    /**
     * 根据执行ID查询详细结果
     */
    private JsonNode queryByExecId(String execId, String projectId) {
        CallGraphExecutionRecord record = templateExecutionRecordService.getCallGraphRecordByExecId(execId);
        if (record == null) {
            return McpToolHelper.createQueryFailResult("执行记录不存在: " + execId);
        }

        // 验证执行记录属于该项目
        if (!projectId.equals(record.getProjectId())) {
            return McpToolHelper.createQueryFailResult("执行记录不属于该项目");
        }

        ObjectNode data = objectMapper.createObjectNode();
        data.put("execId", record.getExecId());
        data.put("projectId", record.getProjectId());
        data.put("templateId", record.getTemplateId());
        data.put("direction", record.getDirection());
        data.put("status", record.getStatus());
        if (record.getDuration() != null) {
            data.put("duration", record.getDuration());
        }
        if (record.getOutputDir() != null) {
            data.put("outputDir", record.getOutputDir());
        }
        if (record.getLogFilePath() != null) {
            data.put("logFilePath", record.getLogFilePath());
        }
        if (record.getErrorMessage() != null) {
            data.put("errorMessage", record.getErrorMessage());
        }
        if (record.getStartTime() != null) {
            data.put("startTime", record.getStartTime().getTime());
        }
        if (record.getEndTime() != null) {
            data.put("endTime", record.getEndTime().getTime());
        }

        // 从call_graph_file_info表查询并返回调用链文件路径Map
        java.util.List<CallGraphFileInfo> fileInfoList = templateExecutionRecordService.getCallGraphFileInfoByRecordId(record.getId());
        if (fileInfoList != null && !fileInfoList.isEmpty()) {
            com.fasterxml.jackson.databind.node.ObjectNode filesNode = objectMapper.createObjectNode();
            for (CallGraphFileInfo fileInfo : fileInfoList) {
                com.fasterxml.jackson.databind.node.ObjectNode infoNode = objectMapper.createObjectNode();
                infoNode.put("origText", fileInfo.getOrigText());
                infoNode.put("filePath", fileInfo.getFilePath());
                filesNode.set(fileInfo.getEntryMethod(), infoNode);
            }
            data.set("methodCallGraphFilePathMap", filesNode);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("success", true);
        response.set("data", data);

        return McpToolHelper.createJsonResult(response);
    }

    /**
     * 查询默认模板的当前执行状态
     */
    private JsonNode queryDefaultTemplateStatus(String projectId, String direction) {
        String defaultTemplateId = executeServiceImpl.getDefaultTemplateId(projectId, direction);

        if (defaultTemplateId == null) {
            ObjectNode data = objectMapper.createObjectNode();
            data.put("executing", false);
            data.put("defaultTemplateExists", false);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.set("data", data);

            return McpToolHelper.createJsonResult(response);
        }

        boolean executing = executeService.isTemplateExecuting(defaultTemplateId);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("executing", executing);
        data.put("defaultTemplateExists", true);

        ExecutionVO executionInfo = executeService.getTemplateExecutionInfo(defaultTemplateId);
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
        } else {
            data.put("hasExecutionRecord", false);
            data.put("hint", "当前无执行记录，请先调用 " + McpToolEnum.EXECUTE_DEFAULT_TEMPLATE.getName() + " 执行调用链生成");
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("success", true);
        response.set("data", data);

        return McpToolHelper.createJsonResult(response);
    }
}
