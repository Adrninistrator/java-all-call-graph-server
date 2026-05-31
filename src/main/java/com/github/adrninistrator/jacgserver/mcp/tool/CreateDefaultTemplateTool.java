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
import com.github.adrninistrator.jacgserver.model.dto.JACGConfigDTO;
import com.github.adrninistrator.jacgserver.model.dto.TemplateDTO;
import com.github.adrninistrator.jacgserver.model.vo.TemplateVO;
import com.github.adrninistrator.jacgserver.service.ProjectService;
import com.github.adrninistrator.jacgserver.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * 在项目下创建默认模板的 MCP Tool
 *
 * AI 可通过此工具在指定项目下创建默认模板（向上或向下）。
 * 每个项目只能有一个向上和一个向下的默认模板。
 * 向上默认模板的描述与ID均为 default_template_4ee，向下为 default_template_4er。
 * 默认模板的入口类/方法自动使用占位符 ${place_holder}，执行时由MCP工具指定实际的入口类/方法。
 * 请求参数只需要指定项目ID和方向，降低功能复杂度。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class CreateDefaultTemplateTool implements McpToolHandler {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ProjectService projectService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.CREATE_DEFAULT_TEMPLATE,
                "在项目下创建默认模板。" +
                "通过direction参数指定方向：callee（向上调用链，描述与ID为 default_template_4ee）或 caller（向下调用链，描述与ID为 default_template_4er）。" +
                "每个项目只能有一个向上和一个向下的默认模板。" +
                "默认模板的入口类/方法自动使用占位符 ${place_holder}，执行时由MCP工具指定实际的入口类/方法。" +
                "请求参数只需要指定项目ID和方向，降低功能复杂度。")
                .addProperty("project_id", "string", "项目ID", true)
                .addProperty("direction", "string", "调用链方向：callee（向上调用链）或 caller（向下调用链）", true);
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

        try {
            // 构造TemplateDTO
            TemplateDTO templateDTO = new TemplateDTO();
            templateDTO.setDescription(Constants.DIRECTION_CALLEE.equals(direction)
                    ? Constants.DEFAULT_TEMPLATE_DESC_4EE : Constants.DEFAULT_TEMPLATE_DESC_4ER);
            templateDTO.setDirection(direction);
            templateDTO.setDefaultTemplate(true);

            // 构造JACG配置（使用默认值）
            JACGConfigDTO jacgConfig = new JACGConfigDTO();
            jacgConfig.setMainConfig(new HashMap<>());
            jacgConfig.setDbConfig(new HashMap<>());
            jacgConfig.setListConfig(new HashMap<>());
            jacgConfig.setSetConfig(new HashMap<>());
            jacgConfig.setElConfig(new HashMap<>());
            templateDTO.setJacgConfig(jacgConfig);

            TemplateVO template = templateService.createTemplate(projectId, templateDTO);

            // 构造返回数据
            ObjectNode data = objectMapper.createObjectNode();
            data.put("templateId", template.getTemplateId());
            data.put("projectId", template.getProjectId());
            data.put("description", template.getDescription());
            data.put("direction", template.getDirection());
            data.put("defaultTemplate", template.getDefaultTemplate() != null ? template.getDefaultTemplate() : false);
            data.put("createTime", template.getCreateTime() != null ? template.getCreateTime() : "");
            data.put("updateTime", template.getUpdateTime() != null ? template.getUpdateTime() : "");

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
