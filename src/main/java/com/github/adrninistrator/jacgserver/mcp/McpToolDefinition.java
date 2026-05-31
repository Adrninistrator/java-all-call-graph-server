package com.github.adrninistrator.jacgserver.mcp;

import com.github.adrninistrator.jacgserver.enums.McpToolEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Tool 定义
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class McpToolDefinition {

    private final String name;
    private final String description;
    private final Map<String, PropertyDef> properties = new HashMap<>();
    private final List<String> required = new ArrayList<>();

    public McpToolDefinition(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * 使用 McpToolEnum 枚举创建 MCP Tool 定义
     *
     * @param toolEnum    MCP工具枚举
     * @param description 工具描述
     */
    public McpToolDefinition(McpToolEnum toolEnum, String description) {
        this.name = toolEnum.getName();
        this.description = description;
    }

    /**
     * 添加属性定义
     *
     * @param name        属性名
     * @param type        属性类型（string、integer、boolean、array 等）
     * @param description 属性描述
     * @param required    是否必填
     * @return 当前定义对象
     */
    public McpToolDefinition addProperty(String name, String type, String description, boolean required) {
        this.properties.put(name, new PropertyDef(type, description, null));
        if (required) {
            this.required.add(name);
        }
        return this;
    }

    /**
     * 添加数组类型属性定义，自动设置 items 字段
     *
     * @param name        属性名
     * @param itemType    数组元素类型（如 "string"、"integer"）
     * @param description 属性描述
     * @param required    是否必填
     * @return 当前定义对象
     */
    public McpToolDefinition addArrayProperty(String name, String itemType, String description, boolean required) {
        this.properties.put(name, new PropertyDef("array", description, itemType));
        if (required) {
            this.required.add(name);
        }
        return this;
    }

    public Map<String, Object> toJsonSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> props = new HashMap<>();
        for (Map.Entry<String, PropertyDef> entry : properties.entrySet()) {
            Map<String, Object> prop = new HashMap<>();
            PropertyDef propertyDef = entry.getValue();
            prop.put("type", propertyDef.type);
            prop.put("description", propertyDef.description);
            // 对 array 类型添加 items 字段，符合 JSON Schema 规范
            if ("array".equals(propertyDef.type) && propertyDef.itemType != null) {
                Map<String, Object> items = new HashMap<>();
                items.put("type", propertyDef.itemType);
                prop.put("items", items);
            }
            props.put(entry.getKey(), prop);
        }
        schema.put("properties", props);

        if (!required.isEmpty()) {
            schema.put("required", new ArrayList<>(required));
        }

        return schema;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    private static class PropertyDef {
        final String type;
        final String description;
        /** 数组元素类型，仅 type=array 时使用 */
        final String itemType;

        PropertyDef(String type, String description, String itemType) {
            this.type = type;
            this.description = description;
            this.itemType = itemType;
        }
    }
}
