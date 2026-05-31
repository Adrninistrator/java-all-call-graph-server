package com.github.adrninistrator.jacgserver.mcp.tool;

import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.exception.BaseException;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.model.dto.JACGConfigDTO;
import com.github.adrninistrator.jacgserver.model.dto.JavaCG2ConfigDTO;
import com.github.adrninistrator.jacgserver.model.dto.ProjectDTO;
import com.github.adrninistrator.jacgserver.model.vo.ProjectVO;
import com.github.adrninistrator.jacgserver.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 创建项目的 MCP Tool
 *
 * AI 可通过此工具创建项目，入参包括项目描述、需要解析的jar文件路径或目录路径、项目根目录等。
 * 对请求参数中的项目根目录使用 File.getCanonicalPath() 规范化。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class CreateProjectTool implements McpToolHandler {

    @Autowired
    private ProjectService projectService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.CREATE_PROJECT,
                "创建项目。入参包括项目描述、需要解析的jar文件路径或目录路径、项目根目录等。" +
                "项目描述为必填参数，jar文件路径或目录路径为必填参数，项目根目录为必填参数。" +
                "对请求参数中的项目根目录使用File.getCanonicalPath()规范化。" +
                "项目根目录代表被解析的代码对应项目的根目录，在全局范围内唯一。")
                .addProperty("description", "string", "项目描述，不能为空且全局唯一", true)
                .addArrayProperty("jar_paths", "string", "需要解析的jar文件路径或目录路径列表，每个元素为一个文件路径或目录路径", true)
                .addProperty("project_root_dir", "string", "项目根目录，代表被解析的代码对应项目的根目录，保存时使用File.getCanonicalPath()规范化，在全局范围内唯一", true);
    }

    @Override
    public JsonNode handle(JsonNode arguments) {
        // 获取项目描述
        String description = arguments.has("description") ? arguments.get("description").asText() : null;
        if (description == null || description.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("项目描述不能为空");
        }

        // 获取jar文件路径或目录路径
        JsonNode jarPathsNode = arguments.get("jar_paths");
        if (jarPathsNode != null && !jarPathsNode.isArray()) {
            return McpToolHelper.createErrorResult("参数jar_paths应为数组类型");
        }
        if (jarPathsNode == null || jarPathsNode.size() == 0) {
            return McpToolHelper.createErrorResult("jar文件路径或目录路径不能为空");
        }

        List<String> jarPaths = new ArrayList<>();
        for (JsonNode node : jarPathsNode) {
            String path = node.asText();
            if (path != null && !path.trim().isEmpty()) {
                jarPaths.add(path.trim());
            }
        }

        if (jarPaths.isEmpty()) {
            return McpToolHelper.createErrorResult("jar文件路径或目录路径不能为空");
        }

        // 获取项目根目录（MCP接口必填）
        String projectRootDir = arguments.has("project_root_dir") ? arguments.get("project_root_dir").asText() : null;

        if (projectRootDir == null || projectRootDir.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("项目根目录不能为空");
        }

        // 构造ProjectDTO
        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setDescription(description.trim());
        projectDTO.setProjectRootDir((projectRootDir != null && !projectRootDir.trim().isEmpty()) ? projectRootDir.trim() : null);
        // 标记项目通过MCP创建
        projectDTO.setCreatedByMcp(true);

        // 构造JavaCG2配置，设置jar路径
        JavaCG2ConfigDTO javacg2Config = new JavaCG2ConfigDTO();
        Map<String, List<String>> listConfig = new HashMap<>();
        listConfig.put(JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR.name(), jarPaths);
        javacg2Config.setListConfig(listConfig);
        javacg2Config.setMainConfig(new HashMap<>());
        javacg2Config.setSetConfig(new HashMap<>());
        javacg2Config.setElConfig(new HashMap<>());
        projectDTO.setJavacg2Config(javacg2Config);

        // 构造JACG配置（使用默认值）
        JACGConfigDTO jacgConfig = new JACGConfigDTO();
        jacgConfig.setMainConfig(new HashMap<>());
        jacgConfig.setDbConfig(new HashMap<>());
        jacgConfig.setListConfig(new HashMap<>());
        jacgConfig.setSetConfig(new HashMap<>());
        jacgConfig.setElConfig(new HashMap<>());
        projectDTO.setJacgConfig(jacgConfig);

        try {
            ProjectVO project = projectService.createProject(projectDTO);

            // 构造返回数据
            ObjectNode data = objectMapper.createObjectNode();
            data.put("projectId", project.getProjectId());
            data.put("description", project.getDescription());
            data.put("projectRootDir", project.getProjectRootDir() != null ? project.getProjectRootDir() : "");
            data.put("createTime", project.getCreateTime() != null ? project.getCreateTime() : "");

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
