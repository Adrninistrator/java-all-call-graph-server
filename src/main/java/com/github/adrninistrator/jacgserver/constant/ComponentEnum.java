package com.github.adrninistrator.jacgserver.constant;

/**
 * 组件枚举
 * 定义java-callgraph2和java-all-call-graph组件的简称和全称常量
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public enum ComponentEnum {

    /**
     * java-callgraph2组件
     */
    JAVACG2("javacg2", "java-callgraph2"),

    /**
     * java-all-call-graph组件
     */
    JACG("jacg", "java-all-call-graph");

    /**
     * 组件简称（用于JSON字段名、MCP参数名等标识符）
     */
    private final String shortName;

    /**
     * 组件全称（用于JAR文件名关键字匹配、说明文字等）
     */
    private final String fullName;

    ComponentEnum(String shortName, String fullName) {
        this.shortName = shortName;
        this.fullName = fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public String getFullName() {
        return fullName;
    }

    /**
     * 根据简称查找组件枚举（不区分大小写）
     *
     * @param shortName 组件简称
     * @return 组件枚举，未找到则返回null
     */
    public static ComponentEnum fromShortName(String shortName) {
        if (shortName == null) {
            return null;
        }
        for (ComponentEnum component : values()) {
            if (component.shortName.equalsIgnoreCase(shortName)) {
                return component;
            }
        }
        return null;
    }
}
