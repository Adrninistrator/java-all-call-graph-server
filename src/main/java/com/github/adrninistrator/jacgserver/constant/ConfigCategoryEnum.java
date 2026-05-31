package com.github.adrninistrator.jacgserver.constant;

/**
 * 配置分类枚举
 * 统一定义配置类型的字符串常量，避免在代码中硬编码
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public enum ConfigCategoryEnum {

    /**
     * 主配置（键值对）
     */
    MAIN_CONFIG("mainConfig"),

    /**
     * 数据库配置（键值对）
     */
    DB_CONFIG("dbConfig"),

    /**
     * List配置（区分顺序）
     */
    LIST_CONFIG("listConfig"),

    /**
     * Set配置（不区分顺序）
     */
    SET_CONFIG("setConfig"),

    /**
     * EL表达式配置
     */
    EL_CONFIG("elConfig");

    private final String value;

    ConfigCategoryEnum(String value) {
        this.value = value;
    }

    /**
     * 获取配置分类的字符串值，用于JSON字段名
     *
     * @return 字符串值
     */
    public String getValue() {
        return value;
    }
}
