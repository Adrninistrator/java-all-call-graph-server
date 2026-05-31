package com.github.adrninistrator.jacgserver.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP Tool 结果工具类
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public final class McpToolHelper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private McpToolHelper() {
    }

    /**
     * 创建文本类型的结果
     */
    public static ObjectNode createTextResult(String text) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode content = objectMapper.createArrayNode();
        ObjectNode textContent = objectMapper.createObjectNode();
        textContent.put("type", "text");
        textContent.put("text", text);
        content.add(textContent);
        result.set("content", content);
        return result;
    }

    /**
     * 创建JSON类型的结果
     * 返回数据使用JSON格式，content中type为text，text为JSON字符串
     */
    public static ObjectNode createJsonResult(ObjectNode jsonData) {
        try {
            String jsonText = objectMapper.writeValueAsString(jsonData);
            return createTextResult(jsonText);
        } catch (Exception e) {
            // JSON序列化失败时，降级为纯文本错误信息
            ObjectNode errorData = objectMapper.createObjectNode();
            errorData.put("success", false);
            errorData.put("error", "JSON序列化失败: " + e.getMessage());
            try {
                return createTextResult(objectMapper.writeValueAsString(errorData));
            } catch (Exception ex) {
                return createTextResult("{\"success\":false,\"error\":\"JSON序列化失败\"}");
            }
        }
    }

    /**
     * 创建错误结果（JSON格式）
     * 用于系统级/协议级错误的返回，isError=true，MCP客户端会将其视为错误
     * 适用场景：参数格式错误、内部异常等
     */
    public static ObjectNode createErrorResult(String errorMessage) {
        ObjectNode errorData = objectMapper.createObjectNode();
        errorData.put("success", false);
        errorData.put("error", errorMessage);
        ObjectNode result = createJsonResult(errorData);
        result.put("isError", true);
        return result;
    }

    /**
     * 创建查询失败结果（JSON格式）
     * 用于查询类接口在资源不存在时的返回，isError=false，success=false
     * "查询不到"是查询的正常结果之一，不应视为错误
     */
    public static ObjectNode createQueryFailResult(String message) {
        ObjectNode failData = objectMapper.createObjectNode();
        failData.put("success", false);
        failData.put("error", message);
        return createJsonResult(failData);
    }

    /**
     * 创建操作失败结果（JSON格式）
     * 用于操作类接口在业务逻辑错误时的返回，isError=false，success=false
     * 适用场景：项目不存在、模板不存在、资源已存在、操作执行失败等业务逻辑错误
     * 与查询类接口保持一致，业务逻辑错误不视为工具执行错误
     */
    public static ObjectNode createOperationFailResult(String message) {
        ObjectNode failData = objectMapper.createObjectNode();
        failData.put("success", false);
        failData.put("error", message);
        return createJsonResult(failData);
    }

    /**
     * 创建操作失败结果-带完整数据（JSON格式）
     * 用于操作类接口在操作执行失败时的返回，isError=false，success=false
     * 同时保留完整的data信息，便于调用方获取失败详情
     */
    public static ObjectNode createOperationFailResultFromResponse(ObjectNode response) {
        return createJsonResult(response);
    }
}
