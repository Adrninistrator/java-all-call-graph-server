package com.github.adrninistrator.jacgserver.enums;

/**
 * MCP 工具枚举
 * 维护所有 MCP 服务的名称和说明，方便在使用说明的服务中使用
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public enum McpToolEnum {

    CHECK_PROJECT_DESCRIPTION("check_project_description", "判断项目描述是否已存在", "Check if project description already exists"),
    CREATE_PROJECT("create_project", "创建项目", "Create a project"),
    DELETE_PROJECT("delete_project", "删除项目（仅允许删除通过MCP创建的项目）", "Delete a project (only MCP-created projects can be deleted)"),
    QUERY_PROJECT_ID("query_project_id", "根据项目根目录查询项目ID", "Query project ID by project root directory"),
    EXECUTE_ANALYSIS("execute_analysis", "对项目执行静态分析", "Execute static analysis on a project"),
    QUERY_ANALYSIS_STATUS("query_analysis_status", "查询项目静态分析执行状态", "Query project static analysis execution status"),
    QUERY_CALL_GRAPH_STATUS("query_call_graph_status", "查询默认模板调用链执行状态（通过direction参数区分向上callee/向下caller）", "Query default template call graph execution status (use direction parameter: callee/caller)"),
    QUERY_DEFAULT_TEMPLATE_INFO("query_default_template_info", "查询项目下的默认模板信息", "Query default template info for a project"),
    CREATE_DEFAULT_TEMPLATE("create_default_template", "创建默认模板（通过direction参数区分向上callee/向下caller）", "Create default template (use direction parameter: callee/caller)"),
    EXECUTE_DEFAULT_TEMPLATE("execute_default_template", "执行默认模板（异步执行，返回执行ID，通过direction参数区分向上callee/向下caller）", "Execute default template (async, returns exec ID, use direction parameter: callee/caller)"),
    GET_SYSTEM_INFO("get_system_info", "获取系统信息（传入include_path=true可获取路径信息）", "Get system info (pass include_path=true for path info)"),
    GET_USAGE_GUIDE("get_usage_guide", "获取当前项目的使用说明", "Get usage guide for this project"),
    GET_CONFIG_GUIDE("get_config_guide", "获取配置参数使用说明", "Get configuration parameter guide"),
    SAVE_PROJECT_CONFIG("save_project_config", "修改项目配置参数", "Save project configuration parameters"),
    SAVE_TEMPLATE_CONFIG("save_template_config", "修改模板配置参数", "Save template configuration parameters"),
    QUERY_CONFIG("query_config", "查询项目或模板的配置参数定义及当前值", "Query project or template configuration parameter definitions and current values"),
    GET_EL_USAGE("get_el_usage", "获取EL表达式使用说明（type=common通用说明/javacg2 JavaCG2说明/jacg JACG说明）", "Get EL expression usage guide (type=common general/javacg2 JavaCG2/jacg JACG)");

    private final String name;
    private final String desc;
    private final String descEn;

    McpToolEnum(String name, String desc, String descEn) {
        this.name = name;
        this.desc = desc;
        this.descEn = descEn;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getDescEn() {
        return descEn;
    }

    /**
     * 根据name获取枚举
     *
     * @param name 工具名称
     * @return 枚举值，未找到返回null
     */
    public static McpToolEnum fromName(String name) {
        if (name == null) {
            return null;
        }
        for (McpToolEnum tool : values()) {
            if (tool.name.equals(name)) {
                return tool;
            }
        }
        return null;
    }
}
