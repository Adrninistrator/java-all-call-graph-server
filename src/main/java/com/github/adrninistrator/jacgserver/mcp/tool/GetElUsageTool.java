package com.github.adrninistrator.jacgserver.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.adrninistrator.jacgserver.enums.McpToolEnum;
import com.github.adrninistrator.jacgserver.mcp.McpToolDefinition;
import com.github.adrninistrator.jacgserver.mcp.McpToolHandler;
import com.github.adrninistrator.jacgserver.mcp.McpToolHelper;
import com.github.adrninistrator.jacgserver.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 获取EL表达式使用说明的 MCP Tool
 *
 * AI 可通过此工具获取EL表达式使用说明文件内容，支持三种类型：
 * - common（默认）：EL表达式通用说明（_el_example/el_usage.md）
 * - javacg2：JavaCG2表达式说明（_el_example/el_usage_javacg2.md）
 * - jacg：JACG表达式说明（_el_example/el_usage_jacg.md）
 * 由于classpath中两个jar包都包含同名资源文件 _el_example/el_usage.md，任意取一个就可以。
 * 该接口仅提供MCP服务方式调用，不提供HTTP接口。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class GetElUsageTool implements McpToolHandler {

    @Autowired
    private ConfigService configService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(McpToolEnum.GET_EL_USAGE,
                "获取EL表达式使用说明文件内容。" +
                "支持获取三种类型的EL表达式说明：" +
                "common（EL表达式通用说明）、" +
                "javacg2（JavaCG2表达式说明）、" +
                "jacg（JACG表达式说明），" +
                "通过type参数指定类型。")
                .addProperty("type", "string", "说明类型，可选值：common（EL表达式通用说明，默认）、javacg2（JavaCG2表达式说明）、jacg（JACG表达式说明）", false);
    }

    @Override
    public JsonNode handle(JsonNode arguments) {
        String typeValue = arguments.has("type") ? arguments.get("type").asText("") : "";
        String type = typeValue.isEmpty() ? "common" : typeValue;

        try {
            String content;
            switch (type) {
                case "common":
                    // EL表达式通用说明，由于classpath中两个jar包都包含同名资源文件 _el_example/el_usage.md，任意使用一个
                    content = configService.getElUsageContent("jacg");
                    break;
                case "javacg2":
                    // JavaCG2表达式说明
                    content = configService.getElUsageComponentContent("javacg2");
                    break;
                case "jacg":
                    // JACG表达式说明
                    content = configService.getElUsageComponentContent("jacg");
                    break;
                default:
                    return McpToolHelper.createOperationFailResult("不支持的type参数值: " + type + "，可选值：common、javacg2、jacg");
            }

            if (content == null) {
                return McpToolHelper.createOperationFailResult("未找到EL表达式使用说明文件: " + type);
            }

            ObjectNode data = objectMapper.createObjectNode();
            data.put("type", type);
            data.put("content", content);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("success", true);
            response.set("data", data);

            return McpToolHelper.createJsonResult(response);
        } catch (Exception e) {
            return McpToolHelper.createErrorResult(e.getMessage());
        }
    }
}
