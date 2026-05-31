package com.github.adrninistrator.jacgserver.mcp.tool;

import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
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
import com.github.adrninistrator.jacgserver.model.dto.JavaCG2ConfigDTO;
import com.github.adrninistrator.jacgserver.model.dto.ProjectDTO;
import com.github.adrninistrator.jacgserver.model.vo.ProjectVO;
import com.github.adrninistrator.jacgserver.service.ConfigService;
import com.github.adrninistrator.jacgserver.service.ProjectService;
import com.github.adrninistrator.jacgserver.util.ConfigReaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 修改项目配置参数的 MCP Tool
 *
 * AI 可通过此工具修改指定项目的配置参数，支持修改java-callgraph2和java-all-call-graph组件对应的配置。
 * 配置参数使用合并方式更新：仅修改传入的配置项，未传入的配置项保持不变。
 * 该接口仅提供MCP服务方式调用，不提供HTTP接口。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class SaveProjectConfigTool implements McpToolHandler {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ConfigService configService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> KNOWN_PARAMS = ConfigParamEnum.projectConfigNames();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.SAVE_PROJECT_CONFIG,
                "修改项目配置参数。支持修改java-callgraph2和java-all-call-graph组件对应的配置，" +
                "使用合并方式更新，仅修改传入的配置项，未传入的配置项保持不变。" +
                "建议先调用query_config（传入project_id）查询配置参数定义及当前值，了解可用的配置参数key和值格式。")
                .addProperty(ConfigParamEnum.PROJECT_ID.getName(), "string", "项目ID", true)
                .addProperty(ConfigParamEnum.JAVACG2_MAIN_CONFIG.getName(), "object", "java-callgraph2主配置，key使用JavaCG2ConfigKeyEnum枚举字段名（如" + JavaCG2ConfigKeyEnum.CKE_PARSE_METHOD_CALL_TYPE_VALUE.name() + "），格式: {\"枚举字段名\": \"配置值\"}", false)
                .addProperty(ConfigParamEnum.JAVACG2_LIST_CONFIG.getName(), "object", "java-callgraph2 List配置（有序列表，顺序有意义），key使用JavaCG2OtherConfigFileUseListEnum枚举字段名，格式: {\"枚举字段名\": [\"值1\", \"值2\"]}", false)
                .addProperty(ConfigParamEnum.JAVACG2_SET_CONFIG.getName(), "object", "java-callgraph2 Set配置（无序集合，自动去重），key使用JavaCG2OtherConfigFileUseSetEnum枚举字段名，格式: {\"枚举字段名\": [\"值1\", \"值2\"]}", false)
                .addProperty(ConfigParamEnum.JAVACG2_EL_CONFIG.getName(), "object", "java-callgraph2 EL表达式配置，key使用JavaCG2ElConfigEnum枚举字段名，格式: {\"枚举字段名\": \"表达式\"}", false)
                .addProperty(ConfigParamEnum.JACG_MAIN_CONFIG.getName(), "object", "java-all-call-graph主配置，key使用ConfigKeyEnum枚举字段名（如" + ConfigKeyEnum.CKE_APP_NAME.name() + "），格式: {\"枚举字段名\": \"配置值\"}", false)
                .addProperty(ConfigParamEnum.JACG_DB_CONFIG.getName(), "object", "java-all-call-graph数据库配置，key使用ConfigDbKeyEnum枚举字段名，格式: {\"枚举字段名\": \"配置值\"}", false)
                .addProperty(ConfigParamEnum.JACG_LIST_CONFIG.getName(), "object", "java-all-call-graph List配置（有序列表，顺序有意义），key使用OtherConfigFileUseListEnum枚举字段名，格式: {\"枚举字段名\": [\"值1\", \"值2\"]}", false)
                .addProperty(ConfigParamEnum.JACG_SET_CONFIG.getName(), "object", "java-all-call-graph Set配置（无序集合，自动去重），key使用OtherConfigFileUseSetEnum枚举字段名，格式: {\"枚举字段名\": [\"值1\", \"值2\"]}", false)
                .addProperty(ConfigParamEnum.JACG_EL_CONFIG.getName(), "object", "java-all-call-graph EL表达式配置，key使用ElConfigEnum枚举字段名，格式: {\"枚举字段名\": \"表达式\"}", false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonNode handle(JsonNode arguments) {
        String projectId = arguments.has(ConfigParamEnum.PROJECT_ID.getName()) ? arguments.get(ConfigParamEnum.PROJECT_ID.getName()).asText() : null;
        if (projectId == null || projectId.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("项目ID不能为空");
        }

        try {
            // 检查项目是否存在
            if (!projectService.projectExists(projectId)) {
                return McpToolHelper.createOperationFailResult("项目不存在: " + projectId);
            }

            // 检查未识别的参数名
            List<String> unrecognizedParams = findUnrecognizedParams(arguments, KNOWN_PARAMS);
            if (!unrecognizedParams.isEmpty()) {
                return McpToolHelper.createOperationFailResult(
                        "存在未识别的参数名: " + unrecognizedParams +
                                "，可用的参数名: " + KNOWN_PARAMS);
            }

            ProjectVO existingProject = projectService.getProject(projectId);

            // 获取项目配置目录
            String projectDir = configService.getProjectConfDir() + "/" + projectId;

            // 读取当前配置
            JavaCG2ConfigDTO javacg2Config = ConfigReaderUtil.readJavaCG2Config(projectDir);
            JACGConfigDTO jacgConfig = ConfigReaderUtil.readJACGConfig(projectDir);

            // 合并 java-callgraph2 配置
            javacg2Config = mergeJavaCG2Config(javacg2Config, arguments);
            // 合并 java-all-call-graph 配置
            jacgConfig = mergeJACGConfig(jacgConfig, arguments);

            // 构造 ProjectDTO
            ProjectDTO projectDTO = new ProjectDTO();
            projectDTO.setDescription(existingProject.getDescription());
            projectDTO.setProjectRootDir(existingProject.getProjectRootDir());
            projectDTO.setJavacg2Config(javacg2Config);
            projectDTO.setJacgConfig(jacgConfig);

            // 保存配置
            projectService.updateProject(projectId, projectDTO);

            // 构造返回数据
            ObjectNode data = objectMapper.createObjectNode();
            data.put("projectId", projectId);
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

    private JavaCG2ConfigDTO mergeJavaCG2Config(JavaCG2ConfigDTO existing, JsonNode arguments) {
        if (existing == null) {
            existing = new JavaCG2ConfigDTO();
        }

        if (arguments.has(ConfigParamEnum.JAVACG2_MAIN_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JAVACG2_MAIN_CONFIG.getName()).isNull()) {
            Map<String, Object> merged = existing.getMainConfig() != null ?
                    new HashMap<>(existing.getMainConfig()) : new HashMap<>();
            mergeMapObject(merged, arguments.get(ConfigParamEnum.JAVACG2_MAIN_CONFIG.getName()));
            existing.setMainConfig(merged);
        }

        if (arguments.has(ConfigParamEnum.JAVACG2_LIST_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JAVACG2_LIST_CONFIG.getName()).isNull()) {
            Map<String, List<String>> merged = existing.getListConfig() != null ?
                    new HashMap<>(existing.getListConfig()) : new HashMap<>();
            mergeMapList(merged, arguments.get(ConfigParamEnum.JAVACG2_LIST_CONFIG.getName()));
            existing.setListConfig(merged);
        }

        if (arguments.has(ConfigParamEnum.JAVACG2_SET_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JAVACG2_SET_CONFIG.getName()).isNull()) {
            Map<String, List<String>> merged = existing.getSetConfig() != null ?
                    new HashMap<>(existing.getSetConfig()) : new HashMap<>();
            mergeMapList(merged, arguments.get(ConfigParamEnum.JAVACG2_SET_CONFIG.getName()));
            existing.setSetConfig(merged);
        }

        if (arguments.has(ConfigParamEnum.JAVACG2_EL_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JAVACG2_EL_CONFIG.getName()).isNull()) {
            Map<String, Object> merged = existing.getElConfig() != null ?
                    new HashMap<>(existing.getElConfig()) : new HashMap<>();
            mergeMapObject(merged, arguments.get(ConfigParamEnum.JAVACG2_EL_CONFIG.getName()));
            existing.setElConfig(merged);
        }

        return existing;
    }

    private JACGConfigDTO mergeJACGConfig(JACGConfigDTO existing, JsonNode arguments) {
        if (existing == null) {
            existing = new JACGConfigDTO();
        }

        if (arguments.has(ConfigParamEnum.JACG_MAIN_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JACG_MAIN_CONFIG.getName()).isNull()) {
            Map<String, Object> merged = existing.getMainConfig() != null ?
                    new HashMap<>(existing.getMainConfig()) : new HashMap<>();
            mergeMapObject(merged, arguments.get(ConfigParamEnum.JACG_MAIN_CONFIG.getName()));
            existing.setMainConfig(merged);
        }

        if (arguments.has(ConfigParamEnum.JACG_DB_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JACG_DB_CONFIG.getName()).isNull()) {
            Map<String, Object> merged = existing.getDbConfig() != null ?
                    new HashMap<>(existing.getDbConfig()) : new HashMap<>();
            mergeMapObject(merged, arguments.get(ConfigParamEnum.JACG_DB_CONFIG.getName()));
            existing.setDbConfig(merged);
        }

        if (arguments.has(ConfigParamEnum.JACG_LIST_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JACG_LIST_CONFIG.getName()).isNull()) {
            Map<String, List<String>> merged = existing.getListConfig() != null ?
                    new HashMap<>(existing.getListConfig()) : new HashMap<>();
            mergeMapList(merged, arguments.get(ConfigParamEnum.JACG_LIST_CONFIG.getName()));
            existing.setListConfig(merged);
        }

        if (arguments.has(ConfigParamEnum.JACG_SET_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JACG_SET_CONFIG.getName()).isNull()) {
            Map<String, List<String>> merged = existing.getSetConfig() != null ?
                    new HashMap<>(existing.getSetConfig()) : new HashMap<>();
            mergeMapList(merged, arguments.get(ConfigParamEnum.JACG_SET_CONFIG.getName()));
            existing.setSetConfig(merged);
        }

        if (arguments.has(ConfigParamEnum.JACG_EL_CONFIG.getName()) && !arguments.get(ConfigParamEnum.JACG_EL_CONFIG.getName()).isNull()) {
            Map<String, Object> merged = existing.getElConfig() != null ?
                    new HashMap<>(existing.getElConfig()) : new HashMap<>();
            mergeMapObject(merged, arguments.get(ConfigParamEnum.JACG_EL_CONFIG.getName()));
            existing.setElConfig(merged);
        }

        return existing;
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
