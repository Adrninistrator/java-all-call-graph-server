package com.github.adrninistrator.jacgserver.mcp.tool;

import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.enums.ConfigParamEnum;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.exception.BaseException;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.model.dto.JACGConfigDTO;
import com.github.adrninistrator.jacgserver.model.dto.TemplateDTO;
import com.github.adrninistrator.jacgserver.model.vo.TemplateVO;
import com.github.adrninistrator.jacgserver.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 修改模板配置参数的 MCP Tool
 *
 * AI 可通过此工具修改指定模板的配置参数，模板只涉及java-all-call-graph组件对应的配置参数。
 * 配置参数使用合并方式更新：仅修改传入的配置项，未传入的配置项保持不变。
 * 该接口仅提供MCP服务方式调用，不提供HTTP接口。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class SaveTemplateConfigTool implements McpToolHandler {

    @Autowired
    private TemplateService templateService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> KNOWN_PARAMS = ConfigParamEnum.templateConfigNames();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.SAVE_TEMPLATE_CONFIG,
                "修改模板配置参数。模板只涉及java-all-call-graph组件对应的配置参数，" +
                "使用合并方式更新，仅修改传入的配置项，未传入的配置项保持不变。" +
                "模板不允许修改数据库配置参数，模板使用项目的数据库配置。" +
                "建议先调用query_config（scene=template，传入template_id）查询配置参数定义及当前值，了解可用的配置参数key和值格式。")
                .addProperty(ConfigParamEnum.TEMPLATE_ID.getName(), "string", "模板ID", true)
                .addProperty(ConfigParamEnum.JACG_MAIN_CONFIG.getName(), "object", "java-all-call-graph主配置，key使用ConfigKeyEnum枚举字段名（如" + ConfigKeyEnum.CKE_APP_NAME.name() + "），格式: {\"枚举字段名\": \"配置值\"}", false)
                .addProperty(ConfigParamEnum.JACG_LIST_CONFIG.getName(), "object", "java-all-call-graph List配置（有序列表，顺序有意义），key使用OtherConfigFileUseListEnum枚举字段名，格式: {\"枚举字段名\": [\"值1\", \"值2\"]}", false)
                .addProperty(ConfigParamEnum.JACG_SET_CONFIG.getName(), "object", "java-all-call-graph Set配置（无序集合，自动去重），key使用OtherConfigFileUseSetEnum枚举字段名，格式: {\"枚举字段名\": [\"值1\", \"值2\"]}", false)
                .addProperty(ConfigParamEnum.JACG_EL_CONFIG.getName(), "object", "java-all-call-graph EL表达式配置，key使用ElConfigEnum枚举字段名，格式: {\"枚举字段名\": \"表达式\"}", false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonNode handle(JsonNode arguments) {
        String templateId = arguments.has(ConfigParamEnum.TEMPLATE_ID.getName()) ? arguments.get(ConfigParamEnum.TEMPLATE_ID.getName()).asText() : null;
        if (templateId == null || templateId.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("模板ID不能为空");
        }

        try {
            // 获取当前模板信息
            TemplateVO existingTemplate;
            try {
                existingTemplate = templateService.getTemplate(templateId);
            } catch (BaseException e) {
                return McpToolHelper.createOperationFailResult(e.getMessage());
            }
            if (existingTemplate == null) {
                return McpToolHelper.createOperationFailResult("模板不存在: " + templateId);
            }

            // 检查未识别的参数名
            List<String> unrecognizedParams = findUnrecognizedParams(arguments, KNOWN_PARAMS);
            if (!unrecognizedParams.isEmpty()) {
                return McpToolHelper.createOperationFailResult(
                        "存在未识别的参数名: " + unrecognizedParams +
                                "，可用的参数名: " + KNOWN_PARAMS);
            }

            // 构建 JACG 配置
            JACGConfigDTO jacgConfig = new JACGConfigDTO();

            if (arguments.has(ConfigParamEnum.JACG_MAIN_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JACG_MAIN_CONFIG.getName()).isNull()) {
                Map<String, Object> merged = new HashMap<>();
                mergeMapObject(merged, arguments.get(ConfigParamEnum.JACG_MAIN_CONFIG.getName()));
                jacgConfig.setMainConfig(merged);
            }

            // 模板不允许修改数据库配置参数
            if (arguments.has(ConfigParamEnum.JACG_DB_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JACG_DB_CONFIG.getName()).isNull()) {
                return McpToolHelper.createOperationFailResult("模板不允许修改数据库配置参数，模板使用项目的数据库配置");
            }

            if (arguments.has(ConfigParamEnum.JACG_LIST_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JACG_LIST_CONFIG.getName()).isNull()) {
                Map<String, List<String>> merged = new HashMap<>();
                mergeMapList(merged, arguments.get(ConfigParamEnum.JACG_LIST_CONFIG.getName()));
                jacgConfig.setListConfig(merged);
            }

            if (arguments.has(ConfigParamEnum.JACG_SET_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JACG_SET_CONFIG.getName()).isNull()) {
                Map<String, List<String>> merged = new HashMap<>();
                mergeMapList(merged, arguments.get(ConfigParamEnum.JACG_SET_CONFIG.getName()));
                jacgConfig.setSetConfig(merged);
            }

            if (arguments.has(ConfigParamEnum.JACG_EL_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JACG_EL_CONFIG.getName()).isNull()) {
                Map<String, Object> merged = new HashMap<>();
                mergeMapObject(merged, arguments.get(ConfigParamEnum.JACG_EL_CONFIG.getName()));
                jacgConfig.setElConfig(merged);
            }

            // 构造 TemplateDTO
            TemplateDTO templateDTO = new TemplateDTO();
            templateDTO.setDescription(existingTemplate.getDescription());
            templateDTO.setDirection(existingTemplate.getDirection());
            templateDTO.setDefaultTemplate(existingTemplate.getDefaultTemplate());
            templateDTO.setJacgConfig(jacgConfig);

            // 保存配置
            templateService.updateTemplate(templateId, templateDTO);

            // 构造返回数据
            ObjectNode data = objectMapper.createObjectNode();
            data.put("templateId", templateId);
            data.put("success", true);

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

    private void mergeMapObject(Map<String, Object> target, JsonNode source) {
        Iterator<String> fieldNames = source.fieldNames();
        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            JsonNode value = source.get(key);
            if (value.isTextual()) {
                target.put(key, value.asText());
            } else if (value.isBoolean()) {
                target.put(key, value.asBoolean());
            } else if (value.isInt()) {
                target.put(key, value.asInt());
            } else if (value.isLong()) {
                target.put(key, value.asLong());
            } else if (value.isDouble()) {
                target.put(key, value.asDouble());
            } else {
                target.put(key, value.asText());
            }
        }
    }

    private void mergeMapList(Map<String, List<String>> target, JsonNode source) {
        Iterator<String> fieldNames = source.fieldNames();
        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            JsonNode array = source.get(key);
            if (array.isArray()) {
                List<String> list = new ArrayList<>();
                for (JsonNode item : array) {
                    list.add(item.asText());
                }
                target.put(key, list);
            }
        }
    }

    private List<String> findUnrecognizedParams(JsonNode arguments, Set<String> knownParams) {
        List<String> unrecognized = new ArrayList<>();
        Iterator<String> fieldNames = arguments.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!knownParams.contains(fieldName)) {
                unrecognized.add(fieldName);
            }
        }
        return unrecognized;
    }
}
