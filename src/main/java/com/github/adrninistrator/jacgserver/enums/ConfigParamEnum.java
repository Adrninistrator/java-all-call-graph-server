package com.github.adrninistrator.jacgserver.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 配置参数名枚举
 * 统一管理 save_project_config 和 save_template_config 接口中使用的参数名
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public enum ConfigParamEnum {

    PROJECT_ID("project_id", "项目ID"),
    TEMPLATE_ID("template_id", "模板ID"),

    JAVACG2_MAIN_CONFIG("javacg2_main_config", "java-callgraph2主配置"),
    JAVACG2_LIST_CONFIG("javacg2_list_config", "java-callgraph2 List配置"),
    JAVACG2_SET_CONFIG("javacg2_set_config", "java-callgraph2 Set配置"),
    JAVACG2_EL_CONFIG("javacg2_el_config", "java-callgraph2 EL表达式配置"),

    JACG_MAIN_CONFIG("jacg_main_config", "java-all-call-graph主配置"),
    JACG_DB_CONFIG("jacg_db_config", "java-all-call-graph数据库配置"),
    JACG_LIST_CONFIG("jacg_list_config", "java-all-call-graph List配置"),
    JACG_SET_CONFIG("jacg_set_config", "java-all-call-graph Set配置"),
    JACG_EL_CONFIG("jacg_el_config", "java-all-call-graph EL表达式配置");

    private final String name;
    private final String desc;

    ConfigParamEnum(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据name获取枚举
     *
     * @param name 参数名
     * @return 枚举值，未找到返回null
     */
    public static ConfigParamEnum fromName(String name) {
        if (name == null) {
            return null;
        }
        for (ConfigParamEnum param : values()) {
            if (param.name.equals(name)) {
                return param;
            }
        }
        return null;
    }

    /**
     * 获取所有参数名的Set集合
     *
     * @return 参数名Set
     */
    public static Set<String> allNames() {
        return Arrays.stream(values())
                .map(ConfigParamEnum::getName)
                .collect(Collectors.toSet());
    }

    /**
     * 获取项目配置相关的参数名Set集合
     *
     * @return 项目配置参数名Set
     */
    public static Set<String> projectConfigNames() {
        return Arrays.stream(new ConfigParamEnum[]{
                PROJECT_ID,
                JAVACG2_MAIN_CONFIG, JAVACG2_LIST_CONFIG, JAVACG2_SET_CONFIG, JAVACG2_EL_CONFIG,
                JACG_MAIN_CONFIG, JACG_DB_CONFIG, JACG_LIST_CONFIG, JACG_SET_CONFIG, JACG_EL_CONFIG
        }).map(ConfigParamEnum::getName).collect(Collectors.toSet());
    }

    /**
     * 获取模板配置相关的参数名Set集合
     *
     * @return 模板配置参数名Set
     */
    public static Set<String> templateConfigNames() {
        return Arrays.stream(new ConfigParamEnum[]{
                TEMPLATE_ID,
                JACG_MAIN_CONFIG, JACG_DB_CONFIG, JACG_LIST_CONFIG, JACG_SET_CONFIG, JACG_EL_CONFIG
        }).map(ConfigParamEnum::getName).collect(Collectors.toSet());
    }
}
