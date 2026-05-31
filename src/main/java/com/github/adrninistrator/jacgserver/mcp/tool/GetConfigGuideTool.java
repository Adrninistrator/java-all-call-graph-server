package com.github.adrninistrator.jacgserver.mcp.tool;

import com.adrninistrator.jacg.conf.enums.ConfigDbKeyEnum;
import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.constant.ComponentEnum;
import com.github.adrninistrator.jacgserver.constant.ConfigCategoryEnum;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import org.springframework.stereotype.Component;

/**
 * 获取配置参数使用说明的 MCP Tool
 *
 * AI 可通过此工具了解配置参数系统的结构，包括组件划分、参数格式和修改方式。
 * 该接口仅提供MCP服务方式调用，不提供HTTP接口。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class GetConfigGuideTool implements McpToolHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.GET_CONFIG_GUIDE,
                "获取配置参数使用说明。说明配置参数分为java-callgraph2、java-all-call-graph组件对应的，" +
                "项目涉及java-callgraph2和java-all-call-graph组件对应的配置参数，模板只涉及java-all-call-graph组件对应的配置参数。" +
                "配置参数分为Map格式（key、value键值对）、List格式（value的格式）、Set格式（value的格式）、EL表达式（value为String格式）。")
                .addProperty("language", "string", "返回语言，可选值：zh（中文，默认）、en（英文）", false);
    }

    @Override
    public JsonNode handle(JsonNode arguments) {
        String language = arguments.has("language") ? arguments.get("language").asText() : "zh";
        boolean isEn = "en".equalsIgnoreCase(language);

        try {
            ObjectNode data = objectMapper.createObjectNode();

            // 组件说明
            ArrayNode components = objectMapper.createArrayNode();

            ObjectNode javacg2 = objectMapper.createObjectNode();
            javacg2.put("name", ComponentEnum.JAVACG2.getFullName());
            javacg2.put("desc", isEn
                    ? "Low-level bytecode parsing engine, responsible for parsing method call relationships in jar/war/class files"
                    : "底层字节码解析引擎，负责解析jar/war/class文件中的方法调用关系");
            javacg2.put("scope", isEn
                    ? "Project level only, not applicable to templates"
                    : "仅项目级别（Project）使用，模板不涉及");
            javacg2.set("configTypes", getJavacg2ConfigTypes(isEn));
            components.add(javacg2);

            ObjectNode jacg = objectMapper.createObjectNode();
            jacg.put("name", ComponentEnum.JACG.getFullName());
            jacg.put("desc", isEn
                    ? "Upper-level call chain analysis service, responsible for generating call chain data based on parsing results"
                    : "上层调用链分析服务，负责基于解析结果生成调用链数据");
            jacg.put("scope", isEn
                    ? "Available at both Project and Template levels"
                    : "项目（Project）和模板（Template）级别均可使用");
            jacg.set("configTypes", getJACGConfigTypes(isEn));
            components.add(jacg);

            data.set("components", components);

            // 配置格式说明
            ArrayNode formats = objectMapper.createArrayNode();

            ObjectNode mapFormat = objectMapper.createObjectNode();
            mapFormat.put("type", isEn ? "Map (Key-Value)" : "Map（键值对）");
            mapFormat.put("description", isEn
                    ? "Key is config parameter name, value is config value (String, Boolean, etc.)"
                    : "key为配置项名称，value为配置值（String、Boolean等类型）");
            mapFormat.put("requestFormat", isEn
                    ? "{\"" + ConfigCategoryEnum.MAIN_CONFIG.getValue() + "\": {\"config_key1\": \"value1\", \"config_key2\": \"value2\"}}"
                    : "{\"" + ConfigCategoryEnum.MAIN_CONFIG.getValue() + "\": {\"配置项名称1\": \"配置值1\", \"配置项名称2\": \"配置值2\"}}");
            formats.add(mapFormat);

            ObjectNode listFormat = objectMapper.createObjectNode();
            listFormat.put("type", isEn ? "List (Ordered List)" : "List（有序列表）");
            listFormat.put("description", isEn
                    ? "Key is config parameter name, value is an ordered list of config values"
                    : "key为配置项名称，value为配置值列表，按顺序排列");
            listFormat.put("requestFormat", isEn
                    ? "{\"" + ConfigCategoryEnum.LIST_CONFIG.getValue() + "\": {\"config_key\": [\"value1\", \"value2\", \"value3\"]}}"
                    : "{\"" + ConfigCategoryEnum.LIST_CONFIG.getValue() + "\": {\"配置项名称\": [\"值1\", \"值2\", \"值3\"]}}");
            formats.add(listFormat);

            ObjectNode setFormat = objectMapper.createObjectNode();
            setFormat.put("type", isEn ? "Set (Unordered Collection)" : "Set（无序集合）");
            setFormat.put("description", isEn
                    ? "Key is config parameter name, value is an unordered list of config values"
                    : "key为配置项名称，value为配置值列表，不区分顺序");
            setFormat.put("requestFormat", isEn
                    ? "{\"" + ConfigCategoryEnum.SET_CONFIG.getValue() + "\": {\"config_key\": [\"value1\", \"value2\"]}}"
                    : "{\"" + ConfigCategoryEnum.SET_CONFIG.getValue() + "\": {\"配置项名称\": [\"值1\", \"值2\"]}}");
            formats.add(setFormat);

            ObjectNode elFormat = objectMapper.createObjectNode();
            elFormat.put("type", isEn ? "EL Expression" : "EL表达式");
            elFormat.put("description", isEn
                    ? "Key is config parameter name, value is an EL expression (String, Boolean, etc.) for dynamic configuration"
                    : "key为配置项名称，value为EL表达式（String、Boolean等类型），用于动态配置");
            elFormat.put("requestFormat", isEn
                    ? "{\"" + ConfigCategoryEnum.EL_CONFIG.getValue() + "\": {\"config_key\": \"expression_content\"}}"
                    : "{\"" + ConfigCategoryEnum.EL_CONFIG.getValue() + "\": {\"配置项名称\": \"表达式内容\"}}");
            formats.add(elFormat);

            data.set("configFormats", formats);

            // 配置参数的key说明
            ObjectNode keyNote = objectMapper.createObjectNode();
            keyNote.put("rule", isEn
                    ? "Config parameter keys use enum field names (e.g. " + ConfigKeyEnum.CKE_APP_NAME.name() + "), not the key descriptions (e.g. app.name)"
                    : "配置参数的key使用枚举字段的名称（如" + ConfigKeyEnum.CKE_APP_NAME.name() + "），不使用枚举字段的key的描述（如app.name）");
            ArrayNode keyExamples = objectMapper.createArrayNode();
            if (isEn) {
                keyExamples.add(ConfigCategoryEnum.MAIN_CONFIG.getValue() + " key: Use ConfigKeyEnum/JavaCG2ConfigKeyEnum enum field names, e.g. " + ConfigKeyEnum.CKE_APP_NAME.name() + ", " + ConfigKeyEnum.CKE_THREAD_NUM.name());
                keyExamples.add(ConfigCategoryEnum.DB_CONFIG.getValue() + " key: Use ConfigDbKeyEnum enum field names, e.g. " + ConfigDbKeyEnum.CDKE_DB_DRIVER_NAME.name() + ", " + ConfigDbKeyEnum.CDKE_DB_URL.name());
                keyExamples.add(ConfigCategoryEnum.LIST_CONFIG.getValue() + " key: Use OtherConfigFileUseListEnum/JavaCG2OtherConfigFileUseListEnum enum field names");
                keyExamples.add(ConfigCategoryEnum.SET_CONFIG.getValue() + " key: Use OtherConfigFileUseSetEnum/JavaCG2OtherConfigFileUseSetEnum enum field names");
                keyExamples.add(ConfigCategoryEnum.EL_CONFIG.getValue() + " key: Use ElConfigEnum/JavaCG2ElConfigEnum enum field names");
            } else {
                keyExamples.add(ConfigCategoryEnum.MAIN_CONFIG.getValue() + "的key：使用ConfigKeyEnum/JavaCG2ConfigKeyEnum的枚举字段名，如 " + ConfigKeyEnum.CKE_APP_NAME.name() + "、" + ConfigKeyEnum.CKE_THREAD_NUM.name());
                keyExamples.add(ConfigCategoryEnum.DB_CONFIG.getValue() + "的key：使用ConfigDbKeyEnum的枚举字段名，如 " + ConfigDbKeyEnum.CDKE_DB_DRIVER_NAME.name() + "、" + ConfigDbKeyEnum.CDKE_DB_URL.name());
                keyExamples.add(ConfigCategoryEnum.LIST_CONFIG.getValue() + "的key：使用OtherConfigFileUseListEnum/JavaCG2OtherConfigFileUseListEnum的枚举字段名");
                keyExamples.add(ConfigCategoryEnum.SET_CONFIG.getValue() + "的key：使用OtherConfigFileUseSetEnum/JavaCG2OtherConfigFileUseSetEnum的枚举字段名");
                keyExamples.add(ConfigCategoryEnum.EL_CONFIG.getValue() + "的key：使用ElConfigEnum/JavaCG2ElConfigEnum的枚举字段名");
            }
            keyNote.set("examples", keyExamples);
            data.set("configKeyNaming", keyNote);

            // 使用说明
            ArrayNode usage = objectMapper.createArrayNode();
            if (isEn) {
                usage.add("This tool provides a macro guide to config parameter structure. For specific parameter definitions (key, name, type, description, enum options, etc.) and current values, please call " + McpToolEnum.QUERY_CONFIG.getName());
                usage.add("Save project config: Call save_project_config tool with project_id and config parameters to modify");
                usage.add("Save template config: Call save_template_config tool with template_id and config parameters (only java-all-call-graph component config supported)");
                usage.add("Query config definitions: Call query_config tool with scene (project/template), optional project_id/template_id to include currentValue for each config parameter, optional config_keys to specify query scope");
                usage.add("query_config returns complete parameter descriptions, including full description, enum options, and numeric ranges");
                usage.add("Config parameters use merge update: only modified parameters are updated, unmodified parameters remain unchanged");
                usage.add("Config format follows java-callgraph2 and java-all-call-graph library standard config file format");
                usage.add("Config parameter keys must use enum field names (e.g. " + ConfigKeyEnum.CKE_APP_NAME.name() + "), not key descriptions (e.g. app.name)");
                usage.add("Default template Set config OCFUSE_METHOD_CLASS_4CALLEE/OCFUSE_METHOD_CLASS_4CALLER cannot be modified (frontend and MCP interface both forbidden)");
                usage.add("Template database config parameters must match the corresponding project's database config parameters; when modifying project database config, template database config is synced accordingly; template database config cannot be modified independently");
            } else {
                usage.add("本工具提供配置参数结构的宏观指导，若需要查看具体的配置参数定义（key、name、type、description、枚举选项等）及当前值，请调用 " + McpToolEnum.QUERY_CONFIG.getName());
                usage.add("修改项目配置：调用 save_project_config 工具，传入 project_id 和需要修改的配置参数");
                usage.add("修改模板配置：调用 save_template_config 工具，传入 template_id 和需要修改的配置参数（仅支持java-all-call-graph组件配置）");
                usage.add("查询配置参数定义：调用 query_config 工具，传入 scene（project/template），可选 project_id/template_id 获取每个配置参数的当前值，可选 config_keys 指定查询范围");
                usage.add("query_config 返回完整的参数说明，包括完整描述、枚举选项和数值范围");
                usage.add("配置参数使用合并方式更新：仅修改传入的配置项，未传入的配置项保持不变");
                usage.add("配置格式遵循java-callgraph2和java-all-call-graph库的标准配置文件格式");
                usage.add("配置参数的key必须使用枚举字段的名称（如 " + ConfigKeyEnum.CKE_APP_NAME.name() + "），不能使用 key 的描述（如 app.name）");
                usage.add("默认模板的Set配置 OCFUSE_METHOD_CLASS_4CALLEE/OCFUSE_METHOD_CLASS_4CALLER 不允许修改（前端页面和MCP服务接口都禁止修改）");
                usage.add("模板的数据库配置参数需要与对应的项目的数据库配置参数相同，在修改项目的数据库配置参数时，会同步按相同方式修改对应模板的数据库配置参数，模板的数据库配置参数不允许修改");
            }
            data.set("usage", usage);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.set("data", data);

            return McpToolHelper.createJsonResult(response);
        } catch (Exception e) {
            return McpToolHelper.createErrorResult(e.getMessage());
        }
    }

    private ArrayNode getJavacg2ConfigTypes(boolean isEn) {
        ArrayNode types = objectMapper.createArrayNode();
        if (isEn) {
            types.add(ConfigCategoryEnum.MAIN_CONFIG.getValue() + " - Main config (Map format), key is config name, value is config value");
            types.add(ConfigCategoryEnum.LIST_CONFIG.getValue() + " - List config (ordered), key is config name, value is list of config values");
            types.add(ConfigCategoryEnum.SET_CONFIG.getValue() + " - Set config (unordered), key is config name, value is list of config values");
            types.add(ConfigCategoryEnum.EL_CONFIG.getValue() + " - EL expression config (Map format), key is config name, value is expression content");
        } else {
            types.add(ConfigCategoryEnum.MAIN_CONFIG.getValue() + " - 主配置（Map格式），key为配置项名称，value为配置值");
            types.add(ConfigCategoryEnum.LIST_CONFIG.getValue() + " - List配置（有序），key为配置项名称，value为配置值列表");
            types.add(ConfigCategoryEnum.SET_CONFIG.getValue() + " - Set配置（无序），key为配置项名称，value为配置值列表");
            types.add(ConfigCategoryEnum.EL_CONFIG.getValue() + " - EL表达式配置（Map格式），key为配置项名称，value为表达式内容");
        }
        return types;
    }

    private ArrayNode getJACGConfigTypes(boolean isEn) {
        ArrayNode types = objectMapper.createArrayNode();
        if (isEn) {
            types.add(ConfigCategoryEnum.MAIN_CONFIG.getValue() + " - Main config (Map format), key is config name, value is config value");
            types.add(ConfigCategoryEnum.DB_CONFIG.getValue() + " - Database config (Map format), key is db config name, value is config value");
            types.add(ConfigCategoryEnum.LIST_CONFIG.getValue() + " - List config (ordered), key is config name, value is list of config values");
            types.add(ConfigCategoryEnum.SET_CONFIG.getValue() + " - Set config (unordered), key is config name, value is list of config values");
            types.add(ConfigCategoryEnum.EL_CONFIG.getValue() + " - EL expression config (Map format), key is config name, value is expression content");
        } else {
            types.add(ConfigCategoryEnum.MAIN_CONFIG.getValue() + " - 主配置（Map格式），key为配置项名称，value为配置值");
            types.add(ConfigCategoryEnum.DB_CONFIG.getValue() + " - 数据库配置（Map格式），key为数据库配置项名称，value为配置值");
            types.add(ConfigCategoryEnum.LIST_CONFIG.getValue() + " - List配置（有序），key为配置项名称，value为配置值列表");
            types.add(ConfigCategoryEnum.SET_CONFIG.getValue() + " - Set配置（无序），key为配置项名称，value为配置值列表");
            types.add(ConfigCategoryEnum.EL_CONFIG.getValue() + " - EL表达式配置（Map格式），key为配置项名称，value为表达式内容");
        }
        return types;
    }
}
