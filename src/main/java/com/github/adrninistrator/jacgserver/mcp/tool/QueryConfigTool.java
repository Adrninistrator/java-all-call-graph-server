package com.github.adrninistrator.jacgserver.mcp.tool;

import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.constant.ConfigCategoryEnum;
import com.github.adrninistrator.jacgserver.enums.ConfigSceneEnum;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.exception.BaseException;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.model.dto.JACGConfigDTO;
import com.github.adrninistrator.jacgserver.model.dto.JavaCG2ConfigDTO;
import com.github.adrninistrator.jacgserver.model.vo.ConfigDefinitionVO;
import com.github.adrninistrator.jacgserver.model.vo.ConfigItemVO;
import com.github.adrninistrator.jacgserver.model.vo.ElAllowedVariableVO;
import com.github.adrninistrator.jacgserver.model.vo.EnumOptionVO;
import com.github.adrninistrator.jacgserver.model.vo.JACGConfigDefinitionVO;
import com.github.adrninistrator.jacgserver.model.vo.JavaCG2ConfigDefinitionVO;
import com.github.adrninistrator.jacgserver.model.vo.OtherConfigItemVO;
import com.github.adrninistrator.jacgserver.service.ConfigService;
import com.github.adrninistrator.jacgserver.service.ProjectService;
import com.github.adrninistrator.jacgserver.service.TemplateService;
import com.github.adrninistrator.jacgserver.util.ConfigReaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 查询配置参数的 MCP Tool
 *
 * AI 可通过此工具查询项目或模板的配置参数定义及当前值。
 * 当指定project_id或template_id时，每个配置参数会同时返回当前值。
 * 配置参数的key使用枚举字段的名称（如ConfigKeyEnum.CKE_APP_NAME）。
 * 返回完整的参数说明，包括完整描述、枚举选项、数值范围等。
 * 不返回visible、editable、required、dependsOn字段。
 * 该接口仅提供MCP服务方式调用，不提供HTTP接口。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class QueryConfigTool implements McpToolHandler {

    @Autowired
    private ConfigService configService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TemplateService templateService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.QUERY_CONFIG,
                "查询项目或模板的配置参数定义及当前值。支持查询项目涉及的java-callgraph2、java-all-call-graph组件对应的配置参数，" +
                "以及模板涉及的java-all-call-graph组件对应的配置参数。" +
                "默认查询全部的配置参数定义，若有指定范围，则只查询指定的一个或多个配置参数定义。" +
                "当指定project_id或template_id时，每个配置参数会同时返回当前使用的值。" +
                "当definition_only=true时仅返回配置参数定义，不返回当前值，也不校验项目或模板是否存在。" +
                "返回完整的参数说明，包括完整描述、枚举选项、数值范围等。")
                .addProperty("scene", "string", "配置场景，可选值：project（项目配置，默认）、template（模板配置）", false)
                .addProperty("project_id", "string", "项目ID", true)
                .addProperty("template_id", "string", "模板ID（scene为template时可传入，传入后返回该模板的当前配置参数值）", false)
                .addProperty("definition_only", "boolean", "仅返回配置参数定义，不返回当前值（默认false，设为true时不会校验项目/模板是否存在）", false)
                .addProperty("config_type", "string", "配置类别过滤，可选值：main（主配置）、db（数据库配置）、list（List配置）、set（Set配置）、el（EL表达式配置），不指定则返回全部", false)
                .addArrayProperty("config_keys", "string", "需要查询的配置参数枚举字段名称列表，可跨类别指定（main/db/list/set/el中的key均可混合指定），不指定则查询全部，如 [\"" + ConfigKeyEnum.CKE_APP_NAME.name() + "\", \"" + ConfigKeyEnum.CKE_THREAD_NUM.name() + "\"]", false);
    }

    @Override
    public JsonNode handle(JsonNode arguments) {
        String sceneStr = arguments.has("scene") ? arguments.get("scene").asText("project") : "project";
        String projectId = arguments.has("project_id") ? arguments.get("project_id").asText(null) : null;
        if (projectId == null || projectId.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("项目ID不能为空");
        }

        String templateId = arguments.has("template_id") ? arguments.get("template_id").asText(null) : null;
        boolean definitionOnly = arguments.has("definition_only") && arguments.get("definition_only").asBoolean(false);
        String configType = arguments.has("config_type") ? arguments.get("config_type").asText(null) : null;
        Set<String> configKeys = null;

        if (arguments.has("config_keys") && arguments.get("config_keys").isArray()) {
            configKeys = new HashSet<>();
            for (JsonNode key : arguments.get("config_keys")) {
                String keyText = key.asText();
                if (keyText != null && !keyText.trim().isEmpty()) {
                    configKeys.add(keyText);
                }
            }
            // 若过滤后为空则视为未指定
            if (configKeys.isEmpty()) {
                configKeys = null;
            }
        }

        try {
            ConfigSceneEnum scene = "template".equalsIgnoreCase(sceneStr)
                    ? ConfigSceneEnum.TEMPLATE : ConfigSceneEnum.PROJECT;

            // 参数校验
            if (templateId != null && !templateId.trim().isEmpty() && scene != ConfigSceneEnum.TEMPLATE) {
                return McpToolHelper.createErrorResult("template_id参数仅在scene为template时可使用");
            }

            // 使用ignoreVisibility=true，MCP接口返回所有配置参数
            ConfigDefinitionVO definitions = configService.getConfigDefinitions(scene, true);

            // 读取当前配置值（definition_only=true时跳过）
            JavaCG2ConfigDTO javacg2Config = null;
            JACGConfigDTO jacgConfig = null;
            String configDir = null;

            if (!definitionOnly) {
                if (scene == ConfigSceneEnum.PROJECT) {
                    // 检查项目是否存在
                    if (!projectService.projectExists(projectId)) {
                        return McpToolHelper.createOperationFailResult("项目不存在: " + projectId);
                    }
                    configDir = configService.getProjectConfDir() + File.separator + projectId;
                    javacg2Config = ConfigReaderUtil.readJavaCG2Config(configDir);
                    jacgConfig = ConfigReaderUtil.readJACGConfig(configDir);
                } else if (scene == ConfigSceneEnum.TEMPLATE && templateId != null && !templateId.trim().isEmpty()) {
                    // 使用project_id精确查找模板目录
                    String templateDir = configService.findTemplateDir(templateId, projectId);
                    if (templateDir == null) {
                        return McpToolHelper.createOperationFailResult("模板不存在: " + templateId);
                    }
                    configDir = templateDir;
                    jacgConfig = ConfigReaderUtil.readJACGConfig(configDir);
                }
            }

            ObjectNode data = objectMapper.createObjectNode();
            data.put("scene", scene.getCode());
            data.put("sceneDesc", scene.getDesc());

            if (scene == ConfigSceneEnum.PROJECT) {
                data.put("projectId", projectId);
            }
            if (templateId != null && !templateId.trim().isEmpty()) {
                data.put("templateId", templateId);
            }
            if (scene == ConfigSceneEnum.TEMPLATE) {
                data.put("projectId", projectId);
            }

            // 处理JavaCG2配置定义（仅在项目场景下）
            if (scene == ConfigSceneEnum.PROJECT && definitions.getJavacg2() != null) {
                ObjectNode javacg2Node = buildJavaCG2ConfigNode(definitions.getJavacg2(), configKeys, javacg2Config, configType);
                if (javacg2Node != null) {
                    data.set("javacg2", javacg2Node);
                }
            }

            // 处理JACG配置定义
            if (definitions.getJacg() != null) {
                ObjectNode jacgNode = buildJacgConfigNode(definitions.getJacg(), configKeys, jacgConfig, scene, configType);
                if (jacgNode != null) {
                    data.set("jacg", jacgNode);
                }
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

    private ObjectNode buildJavaCG2ConfigNode(JavaCG2ConfigDefinitionVO javacg2, Set<String> configKeys,
                                              JavaCG2ConfigDTO currentConfig, String configType) {
        ObjectNode node = objectMapper.createObjectNode();

        if (configType == null || "main".equalsIgnoreCase(configType)) {
            ArrayNode mainConfig = convertConfigItems(javacg2.getMainConfig(), configKeys,
                    currentConfig != null ? currentConfig.getMainConfig() : null, false);
            if (mainConfig != null) {
                node.set(ConfigCategoryEnum.MAIN_CONFIG.getValue(), mainConfig);
            }
        }
        if (configType == null || "list".equalsIgnoreCase(configType)) {
            ArrayNode listConfig = convertOtherConfigItems(javacg2.getListConfig(), configKeys,
                    currentConfig != null ? currentConfig.getListConfig() : null, ConfigSceneEnum.PROJECT);
            if (listConfig != null) {
                node.set(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);
            }
        }
        if (configType == null || "set".equalsIgnoreCase(configType)) {
            ArrayNode setConfig = convertOtherConfigItems(javacg2.getSetConfig(), configKeys,
                    currentConfig != null ? currentConfig.getSetConfig() : null, ConfigSceneEnum.PROJECT);
            if (setConfig != null) {
                node.set(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);
            }
        }
        if (configType == null || "el".equalsIgnoreCase(configType)) {
            ArrayNode elConfig = convertElConfigItems(javacg2.getElConfig(), configKeys,
                    currentConfig != null ? currentConfig.getElConfig() : null);
            if (elConfig != null) {
                node.set(ConfigCategoryEnum.EL_CONFIG.getValue(), elConfig);
            }
        }
        return node.size() > 0 ? node : null;
    }

    private ObjectNode buildJacgConfigNode(JACGConfigDefinitionVO jacg, Set<String> configKeys,
                                           JACGConfigDTO currentConfig, ConfigSceneEnum scene, String configType) {
        ObjectNode node = objectMapper.createObjectNode();

        if (configType == null || "main".equalsIgnoreCase(configType)) {
            ArrayNode mainConfig = convertConfigItems(jacg.getMainConfig(), configKeys,
                    currentConfig != null ? currentConfig.getMainConfig() : null, false);
            if (mainConfig != null) {
                node.set(ConfigCategoryEnum.MAIN_CONFIG.getValue(), mainConfig);
            }
        }
        // 模板场景下dbConfig为只读，继承自项目
        if (configType == null || "db".equalsIgnoreCase(configType)) {
            ArrayNode dbConfig = convertConfigItems(jacg.getDbConfig(), configKeys,
                    currentConfig != null ? currentConfig.getDbConfig() : null, scene == ConfigSceneEnum.TEMPLATE);
            if (dbConfig != null) {
                if (scene == ConfigSceneEnum.TEMPLATE) {
                    node.put(ConfigCategoryEnum.DB_CONFIG.getValue() + "__note", "以下数据库配置参数属于项目配置，模板使用项目的数据库配置，不允许独立修改");
                }
                node.set(ConfigCategoryEnum.DB_CONFIG.getValue(), dbConfig);
            }
        }
        if (configType == null || "list".equalsIgnoreCase(configType)) {
            ArrayNode listConfig = convertOtherConfigItems(jacg.getListConfig(), configKeys,
                    currentConfig != null ? currentConfig.getListConfig() : null, scene);
            if (listConfig != null) {
                node.set(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);
            }
        }
        if (configType == null || "set".equalsIgnoreCase(configType)) {
            ArrayNode setConfig = convertOtherConfigItems(jacg.getSetConfig(), configKeys,
                    currentConfig != null ? currentConfig.getSetConfig() : null, scene);
            if (setConfig != null) {
                node.set(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);
            }
        }
        // JACG的elConfig也是OtherConfigItemVO类型，但EL配置的currentValue为字符串而非数组
        ArrayNode elConfig = convertElConfigItems(jacg.getElConfig(), configKeys,
                currentConfig != null ? currentConfig.getElConfig() : null);
        if (elConfig != null) {
            node.set(ConfigCategoryEnum.EL_CONFIG.getValue(), elConfig);
        }
        return node.size() > 0 ? node : null;
    }

    /**
     * 转换EL配置项列表为JSON数组，并附带当前值
     * EL配置的currentValue为字符串（非数组），与List/Set配置不同
     * 返回EL表达式允许使用的变量信息，使AI理解当前表达式怎样使用
     *
     * @param items         配置项定义列表
     * @param configKeys    需要过滤的key集合
     * @param currentValues 当前值Map（key为枚举字段名称，value为当前值）
     */
    private ArrayNode convertElConfigItems(List<OtherConfigItemVO> items, Set<String> configKeys, Map<String, Object> currentValues) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        ArrayNode array = objectMapper.createArrayNode();
        for (OtherConfigItemVO item : items) {
            if (configKeys != null && !configKeys.isEmpty() && !configKeys.contains(item.getKey())) {
                continue;
            }
            ObjectNode node = objectMapper.createObjectNode();
            node.put("key", item.getKey());
            node.put("name", item.getName());
            // 返回完整描述
            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                ArrayNode descArray = objectMapper.createArrayNode();
                for (String desc : item.getDescription()) {
                    descArray.add(desc);
                }
                node.set("description", descArray);
            }
            // 返回EL表达式允许使用的变量信息
            if (item.getElAllowedVariables() != null && !item.getElAllowedVariables().isEmpty()) {
                ArrayNode varsArray = objectMapper.createArrayNode();
                for (ElAllowedVariableVO varVO : item.getElAllowedVariables()) {
                    ObjectNode varNode = objectMapper.createObjectNode();
                    varNode.put("enumConstantName", varVO.getEnumConstantName());
                    varNode.put("variableName", varVO.getVariableName());
                    varNode.put("type", varVO.getType());
                    varNode.put("prefixWithNum", varVO.isPrefixWithNum());
                    if (varVO.getDescriptions() != null && !varVO.getDescriptions().isEmpty()) {
                        ArrayNode varDescArray = objectMapper.createArrayNode();
                        for (String desc : varVO.getDescriptions()) {
                            varDescArray.add(desc);
                        }
                        varNode.set("descriptions", varDescArray);
                    }
                    if (varVO.getValueExamples() != null && !varVO.getValueExamples().isEmpty()) {
                        ArrayNode exampleArray = objectMapper.createArrayNode();
                        for (String example : varVO.getValueExamples()) {
                            exampleArray.add(example);
                        }
                        varNode.set("valueExamples", exampleArray);
                    }
                    varsArray.add(varNode);
                }
                node.set("allowedVariables", varsArray);
            }
            // EL配置的当前值为字符串
            if (currentValues != null && currentValues.containsKey(item.getKey())) {
                Object value = currentValues.get(item.getKey());
                node.put("currentValue", value != null ? value.toString() : "");
            }
            array.add(node);
        }
        return array.size() > 0 ? array : null;
    }

    /**
     * 转换ConfigItemVO列表为JSON数组，并附带当前值
     * 返回完整的参数说明，包括完整描述、枚举选项、数值范围
     * 不返回visible、editable、required、dependsOn字段
     *
     * @param items        配置项定义列表
     * @param configKeys   需要过滤的key集合
     * @param currentValues 当前值Map（key为枚举字段名称，value为当前值）
     * @param readOnly     是否为只读（模板场景下dbConfig为只读）
     */
    private ArrayNode convertConfigItems(List<ConfigItemVO> items, Set<String> configKeys, Map<String, Object> currentValues, boolean readOnly) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        ArrayNode array = objectMapper.createArrayNode();
        for (ConfigItemVO item : items) {
            if (configKeys != null && !configKeys.isEmpty() && !configKeys.contains(item.getKey())) {
                continue;
            }
            ObjectNode node = objectMapper.createObjectNode();
            node.put("key", item.getKey());
            node.put("name", item.getName());
            node.put("type", item.getType());
            node.put("defaultValue", item.getDefaultValue() != null ? item.getDefaultValue() : "");
            // 返回完整描述（所有行）
            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                ArrayNode descArray = objectMapper.createArrayNode();
                for (String desc : item.getDescription()) {
                    descArray.add(desc);
                }
                node.set("description", descArray);
            }
            // 返回枚举选项
            if (item.getEnumOptions() != null && !item.getEnumOptions().isEmpty()) {
                ArrayNode enumArray = objectMapper.createArrayNode();
                for (EnumOptionVO option : item.getEnumOptions()) {
                    ObjectNode optionNode = objectMapper.createObjectNode();
                    optionNode.put("value", option.getValue());
                    optionNode.put("description", option.getDescription());
                    enumArray.add(optionNode);
                }
                node.set("enumOptions", enumArray);
            }
            // 返回数值范围
            if (item.getMinValue() != null) {
                node.put("minValue", item.getMinValue());
            }
            if (item.getMaxValue() != null) {
                node.put("maxValue", item.getMaxValue());
            }
            // 添加当前值
            if (currentValues != null && currentValues.containsKey(item.getKey())) {
                Object value = currentValues.get(item.getKey());
                node.put("currentValue", value != null ? value.toString() : "");
            }
            // 只读标记
            if (readOnly) {
                node.put("readOnly", true);
            }
            array.add(node);
        }
        return array.size() > 0 ? array : null;
    }

    /**
     * 转换OtherConfigItemVO列表为JSON数组，并附带当前值
     * 返回完整的参数说明，包括完整描述
     * 不返回visible、editable字段
     * 模板场景下不可编辑的配置项（editable=false）标记为readOnly
     *
     * @param items         配置项定义列表
     * @param configKeys    需要过滤的key集合
     * @param currentValues 当前值Map（key为枚举字段名称，value为当前值列表）
     * @param scene         配置场景（模板场景下不可编辑的配置项标记readOnly）
     */
    private ArrayNode convertOtherConfigItems(List<OtherConfigItemVO> items, Set<String> configKeys, Map<String, List<String>> currentValues, ConfigSceneEnum scene) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        ArrayNode array = objectMapper.createArrayNode();
        for (OtherConfigItemVO item : items) {
            if (configKeys != null && !configKeys.isEmpty() && !configKeys.contains(item.getKey())) {
                continue;
            }
            ObjectNode node = objectMapper.createObjectNode();
            node.put("key", item.getKey());
            node.put("name", item.getName());
            // 返回完整描述（所有行）
            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                ArrayNode descArray = objectMapper.createArrayNode();
                for (String desc : item.getDescription()) {
                    descArray.add(desc);
                }
                node.set("description", descArray);
            }
            // 模板场景下，不可编辑的配置项标记为readOnly
            if (scene == ConfigSceneEnum.TEMPLATE && item.getEditable() != null && !item.getEditable()) {
                node.put("readOnly", true);
            }
            // 添加当前值（List/Set/EL配置的当前值为列表形式）
            if (currentValues != null && currentValues.containsKey(item.getKey())) {
                ArrayNode currentValueArray = objectMapper.createArrayNode();
                for (String val : currentValues.get(item.getKey())) {
                    currentValueArray.add(val);
                }
                node.set("currentValue", currentValueArray);
            }
            array.add(node);
        }
        return array.size() > 0 ? array : null;
    }
}
