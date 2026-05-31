package com.github.adrninistrator.jacgserver.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.constant.Constants;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.model.vo.TemplateVO;
import com.github.adrninistrator.jacgserver.service.ProjectService;
import com.github.adrninistrator.jacgserver.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询项目下的默认模板信息的 MCP Tool
 *
 * AI 可通过此工具查询指定项目的默认模板信息，向上与向下的默认模板都返回。
 * 该接口仅提供MCP服务方式调用，不提供HTTP接口。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class QueryDefaultTemplateInfoTool implements McpToolHandler {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ProjectService projectService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.QUERY_DEFAULT_TEMPLATE_INFO,
                "查询项目下的默认模板信息，向上与向下的默认模板都返回。" +
                "仅返回默认模板信息，不返回非默认模板。")
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

            ObjectNode data = objectMapper.createObjectNode();

            // 获取项目下所有模板
            List<TemplateVO> templates = templateService.listTemplates(projectId);

            // 查找向上默认模板
            TemplateVO calleeTemplate = null;
            TemplateVO callerTemplate = null;
            for (TemplateVO template : templates) {
                if (Boolean.TRUE.equals(template.getDefaultTemplate())) {
                    if (Constants.DIRECTION_CALLEE.equals(template.getDirection())) {
                        calleeTemplate = template;
                    } else if (Constants.DIRECTION_CALLER.equals(template.getDirection())) {
                        callerTemplate = template;
                    }
                }
            }

            // 构建向上默认模板数据
            ObjectNode calleeNode = objectMapper.createObjectNode();
            calleeNode.put("exists", calleeTemplate != null);
            if (calleeTemplate != null) {
                calleeNode.put("templateId", calleeTemplate.getTemplateId());
                calleeNode.put("description", calleeTemplate.getDescription() != null ? calleeTemplate.getDescription() : "");
            }
            data.set("calleeTemplate", calleeNode);

            // 构建向下默认模板数据
            ObjectNode callerNode = objectMapper.createObjectNode();
            callerNode.put("exists", callerTemplate != null);
            if (callerTemplate != null) {
                callerNode.put("templateId", callerTemplate.getTemplateId());
                callerNode.put("description", callerTemplate.getDescription() != null ? callerTemplate.getDescription() : "");
            }
            data.set("callerTemplate", callerNode);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.set("data", data);

            return McpToolHelper.createJsonResult(response);
        } catch (Exception e) {
            return McpToolHelper.createErrorResult(e.getMessage());
        }
    }
}
