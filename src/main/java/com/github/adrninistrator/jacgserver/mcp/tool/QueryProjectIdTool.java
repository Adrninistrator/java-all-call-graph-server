package com.github.adrninistrator.jacgserver.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 根据项目根目录查询项目ID的 MCP Tool
 *
 * AI 可通过此工具根据项目根目录查询对应的项目ID和项目信息。
 * 请求参数中的项目根目录会使用 File.getCanonicalPath() 规范化。
 * 返回数据使用JSON格式。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class QueryProjectIdTool implements McpToolHandler {

    @Autowired
    private ProjectService projectService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.QUERY_PROJECT_ID,
                "根据项目根目录查询项目ID和项目信息。项目根目录代表被解析的代码对应项目的根目录。" +
                "若查找到匹配的项目则返回对应的项目ID及其他项目信息，若未查找到则返回未查找到。")
                .addProperty("project_root_dir", "string", "项目根目录路径，代表被解析的代码对应项目的根目录", true);
    }

    @Override
    public JsonNode handle(JsonNode arguments) {
        String projectRootDir = arguments.has("project_root_dir") ? arguments.get("project_root_dir").asText() : null;

        if (projectRootDir == null || projectRootDir.trim().isEmpty()) {
            return McpToolHelper.createErrorResult("项目根目录不能为空");
        }

        Map<String, Object> queryResult = projectService.queryProjectByRootDir(projectRootDir);
        boolean found = Boolean.TRUE.equals(queryResult.get("found"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("found", found);

        if (found) {
            data.put("projectId", String.valueOf(queryResult.get("projectId")));
            data.put("description", String.valueOf(queryResult.get("description")));
            data.put("projectRootDir", String.valueOf(queryResult.get("projectRootDir")));
            data.put("createTime", String.valueOf(queryResult.get("createTime")));
            data.put("updateTime", String.valueOf(queryResult.get("updateTime")));
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("success", true);
        response.set("data", data);

        return McpToolHelper.createJsonResult(response);
    }
}
