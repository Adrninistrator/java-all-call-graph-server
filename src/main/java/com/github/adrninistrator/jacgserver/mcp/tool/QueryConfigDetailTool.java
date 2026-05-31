package com.github.adrninistrator.jacgserver.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.constant.ComponentEnum;
import com.github.adrninistrator.jacgserver.constant.ConfigCategoryEnum;
import com.github.adrninistrator.jacgserver.enums.ConfigSceneEnum;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.model.vo.ConfigDefinitionVO;
import com.github.adrninistrator.jacgserver.model.vo.ConfigItemVO;
import com.github.adrninistrator.jacgserver.model.vo.EnumOptionVO;
import com.github.adrninistrator.jacgserver.model.vo.JACGConfigDefinitionVO;
import com.github.adrninistrator.jacgserver.model.vo.JavaCG2ConfigDefinitionVO;
import com.github.adrninistrator.jacgserver.model.vo.OtherConfigItemVO;
import com.github.adrninistrator.jacgserver.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 查询配置参数详细说明的 MCP Tool（已废弃，合并到query_config中）
 *
 * @author adrninistrator
 * @since 1.0.0
 * @deprecated 已合并到query_config接口，query_config现在返回完整的参数说明
 */
@Deprecated
public class QueryConfigDetailTool implements McpToolHandler {

    @Autowired
    private ConfigService configService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 支持的组件值
     */
    private static final Set<String> VALID_COMPONENTS = new HashSet<>(Arrays.asList(
            ComponentEnum.JAVACG2.getShortName(), ComponentEnum.JACG.getShortName()));

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition("query_config_detail",
                "查询某个配置参数的详细说明。query_config接口返回的配置参数说明不完整（仅返回描述的第一行），" +
                "本接口返回配置参数的完整描述及其他详细信息。" +
                "请求参数包括组件（javacg2/jacg）、配置文件类型（mainConfig/listConfig/setConfig/elConfig/dbConfig）、配置参数枚举名称。" +
                "该接口仅提供MCP服务方式调用，不提供HTTP接口。")
                .addProperty("component", "string", "组件，可选值：javacg2（java-callgraph2）、jacg（java-all-call-graph）", true)
                .addProperty("config_category", "string", "配置文件类型，可选值：" + ConfigCategoryEnum.MAIN_CONFIG.getValue() + "、" +
                        ConfigCategoryEnum.DB_CONFIG.getValue() + "、" + ConfigCategoryEnum.LIST_CONFIG.getValue() + "、" +
                        ConfigCategoryEnum.SET_CONFIG.getValue() + "、" + ConfigCategoryEnum.EL_CONFIG.getValue(), true)
                .addProperty("config_key", "string", "配置参数枚举字段名称，如 CKE_APP_NAME、OCFULE_JAR_DIR 等", true);
    }

    @Override
    public JsonNode handle(JsonNode arguments) {
        String component = arguments.has("component") ? arguments.get("component").asText(null) : null;
        String configCategory = arguments.has("config_category") ? arguments.get("config_category").asText(null) : null;
        String configKey = arguments.has("config_key") ? arguments.get("config_key").asText(null) : null;

        // 参数校验
        if (component == null || component.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("参数component不能为空");
        }
        if (configCategory == null || configCategory.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("参数config_category不能为空");
        }
        if (configKey == null || configKey.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("参数config_key不能为空");
        }

        component = component.trim().toLowerCase();
        configCategory = configCategory.trim();
        configKey = configKey.trim();

        if (!VALID_COMPONENTS.contains(component)) {
            return McpToolHelper.createErrorResult("参数component无效，可选值：" + String.join("、", VALID_COMPONENTS));
        }

        if (!isValidConfigCategory(configCategory)) {
            return McpToolHelper.createErrorResult("参数config_category无效，可选值：" +
                    Arrays.stream(ConfigCategoryEnum.values()).map(ConfigCategoryEnum::getValue).collect(Collectors.joining("、")));
        }

        try {
            // javacg2不支持dbConfig
            if (ComponentEnum.JAVACG2.getShortName().equals(component) && ConfigCategoryEnum.DB_CONFIG.getValue().equals(configCategory)) {
                return McpToolHelper.createOperationFailResult(ComponentEnum.JAVACG2.getShortName() + "组件不支持dbConfig配置分类");
            }

            // 获取配置定义
            ConfigDefinitionVO definitions = configService.getConfigDefinitions(ConfigSceneEnum.PROJECT);

            // 根据组件和配置分类查找配置项
            Object foundItem = findConfigItem(definitions, component, configCategory, configKey);

            if (foundItem == null) {
                return McpToolHelper.createOperationFailResult("未找到配置参数: component=" + component +
                        ", config_category=" + configCategory + ", config_key=" + configKey);
            }

            // 构建返回结果
            ObjectNode data = objectMapper.createObjectNode();
            data.put("component", component);
            data.put("configCategory", configCategory);
            data.put("configKey", configKey);

            if (foundItem instanceof ConfigItemVO) {
                buildConfigItemDetail(data, (ConfigItemVO) foundItem);
            } else if (foundItem instanceof OtherConfigItemVO) {
                buildOtherConfigItemDetail(data, (OtherConfigItemVO) foundItem);
            }

            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.set("data", data);

            return McpToolHelper.createJsonResult(response);
        } catch (Exception e) {
            return McpToolHelper.createErrorResult(e.getMessage());
        }
    }

    /**
     * 校验config_category是否合法
     */
    private boolean isValidConfigCategory(String configCategory) {
        for (ConfigCategoryEnum category : ConfigCategoryEnum.values()) {
            if (category.getValue().equals(configCategory)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据组件和配置分类查找配置项
     */
    private Object findConfigItem(ConfigDefinitionVO definitions, String component, String configCategory, String configKey) {
        if (ComponentEnum.JAVACG2.getShortName().equals(component)) {
            JavaCG2ConfigDefinitionVO javacg2 = definitions.getJavacg2();
            if (javacg2 == null) {
                return null;
            }
            return findInJavaCG2(javacg2, configCategory, configKey);
        } else {
            // jacg
            JACGConfigDefinitionVO jacg = definitions.getJacg();
            if (jacg == null) {
                return null;
            }
            return findInJacg(jacg, configCategory, configKey);
        }
    }

    private Object findInJavaCG2(JavaCG2ConfigDefinitionVO javacg2, String configCategory, String configKey) {
        if (ConfigCategoryEnum.MAIN_CONFIG.getValue().equals(configCategory)) {
            return findInConfigItemList(javacg2.getMainConfig(), configKey);
        } else if (ConfigCategoryEnum.LIST_CONFIG.getValue().equals(configCategory)) {
            return findInOtherConfigItemList(javacg2.getListConfig(), configKey);
        } else if (ConfigCategoryEnum.SET_CONFIG.getValue().equals(configCategory)) {
            return findInOtherConfigItemList(javacg2.getSetConfig(), configKey);
        } else if (ConfigCategoryEnum.EL_CONFIG.getValue().equals(configCategory)) {
            return findInOtherConfigItemList(javacg2.getElConfig(), configKey);
        }
        return null;
    }

    private Object findInJacg(JACGConfigDefinitionVO jacg, String configCategory, String configKey) {
        if (ConfigCategoryEnum.MAIN_CONFIG.getValue().equals(configCategory)) {
            return findInConfigItemList(jacg.getMainConfig(), configKey);
        } else if (ConfigCategoryEnum.DB_CONFIG.getValue().equals(configCategory)) {
            return findInConfigItemList(jacg.getDbConfig(), configKey);
        } else if (ConfigCategoryEnum.LIST_CONFIG.getValue().equals(configCategory)) {
            return findInOtherConfigItemList(jacg.getListConfig(), configKey);
        } else if (ConfigCategoryEnum.SET_CONFIG.getValue().equals(configCategory)) {
            return findInOtherConfigItemList(jacg.getSetConfig(), configKey);
        } else if (ConfigCategoryEnum.EL_CONFIG.getValue().equals(configCategory)) {
            return findInOtherConfigItemList(jacg.getElConfig(), configKey);
        }
        return null;
    }

    private ConfigItemVO findInConfigItemList(List<ConfigItemVO> items, String configKey) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        for (ConfigItemVO item : items) {
            if (configKey.equals(item.getKey())) {
                return item;
            }
        }
        return null;
    }

    private OtherConfigItemVO findInOtherConfigItemList(List<OtherConfigItemVO> items, String configKey) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        for (OtherConfigItemVO item : items) {
            if (configKey.equals(item.getKey())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 构建ConfigItemVO（mainConfig/dbConfig）的详细返回
     */
    private void buildConfigItemDetail(ObjectNode data, ConfigItemVO item) {
        data.put("name", item.getName());
        if (item.getType() != null) {
            data.put("type", item.getType());
        }
        if (item.getDefaultValue() != null) {
            data.put("defaultValue", item.getDefaultValue());
        }
        if (item.getFileName() != null) {
            data.put("fileName", item.getFileName());
        }

        // 完整描述
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            ArrayNode descArray = objectMapper.createArrayNode();
            for (String desc : item.getDescription()) {
                descArray.add(desc);
            }
            data.set("description", descArray);
        }

        // 枚举选项
        if (item.getEnumOptions() != null && !item.getEnumOptions().isEmpty()) {
            ArrayNode enumArray = objectMapper.createArrayNode();
            for (EnumOptionVO option : item.getEnumOptions()) {
                ObjectNode optionNode = objectMapper.createObjectNode();
                optionNode.put("value", option.getValue());
                optionNode.put("description", option.getDescription());
                enumArray.add(optionNode);
            }
            data.set("enumOptions", enumArray);
        }

        // 依赖关系
        if (item.getDependsOn() != null) {
            ObjectNode dependsNode = objectMapper.createObjectNode();
            dependsNode.put("paramKey", item.getDependsOn().getParamKey());
            dependsNode.put("paramValue", item.getDependsOn().getParamValue());
            dependsNode.put("action", item.getDependsOn().getAction());
            data.set("dependsOn", dependsNode);
        }

        // 数值范围
        if (item.getMinValue() != null) {
            data.put("minValue", item.getMinValue());
        }
        if (item.getMaxValue() != null) {
            data.put("maxValue", item.getMaxValue());
        }
    }

    /**
     * 构建OtherConfigItemVO（listConfig/setConfig/elConfig）的详细返回
     */
    private void buildOtherConfigItemDetail(ObjectNode data, OtherConfigItemVO item) {
        data.put("name", item.getName());
        if (item.getFileName() != null) {
            data.put("fileName", item.getFileName());
        }
        if (item.getVisible() != null) {
            data.put("visible", item.getVisible());
        }

        // 完整描述
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            ArrayNode descArray = objectMapper.createArrayNode();
            for (String desc : item.getDescription()) {
                descArray.add(desc);
            }
            data.set("description", descArray);
        }
    }
}
