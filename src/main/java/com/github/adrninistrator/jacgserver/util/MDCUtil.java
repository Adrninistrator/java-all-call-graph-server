package com.github.adrninistrator.jacgserver.util;

import org.slf4j.MDC;

/**
 * MDC工具类
 * 
 * 用于管理动态日志文件的MDC变量
 * 在调用java-callgraph2、java-all-call-graph库之前，需要调用此工具类设置MDC变量
 * 
 * @author adrninistrator
 * @since 1.0.0
 */
public final class MDCUtil {

    /**
     * 默认projectId（用于非执行场景的日志）
     */
    public static final String DEFAULT_PROJECT_ID = "default";

    /**
     * 默认logFileName（用于非执行场景的日志）
     */
    public static final String DEFAULT_LOG_FILE_NAME = "default";

    private MDCUtil() {
        // 私有构造函数
    }

    /**
     * 设置执行静态分析的MDC变量
     * 
     * @param projectId 项目ID
     */
    public static void setWriteDbMDC(String projectId) {
        MDC.put("projectId", projectId);
        MDC.put("logFileName", "writeDb");
    }

    /**
     * 设置执行调用链生成的MDC变量
     * 
     * @param projectId 项目ID
     * @param execId 执行ID
     */
    public static void setCallGraphMDC(String projectId, String execId) {
        MDC.put("projectId", projectId);
        MDC.put("logFileName", execId);
    }

    /**
     * 设置默认MDC变量
     * 用于非执行场景（如读取配置、获取配置定义等）调用库代码时
     */
    public static void setDefaultMDC() {
        MDC.put("projectId", DEFAULT_PROJECT_ID);
        MDC.put("logFileName", DEFAULT_LOG_FILE_NAME);
    }

    /**
     * 设置项目创建时的MDC变量
     * 用于创建项目时，使日志写入项目对应的日志目录
     *
     * @param projectId 项目ID
     */
    public static void setProjectCreateMDC(String projectId) {
        MDC.put("projectId", projectId);
        MDC.put("logFileName", "project_create");
    }

    /**
     * 设置模板创建时的MDC变量
     * 用于创建模板时，使日志写入项目对应的日志目录
     *
     * @param projectId 项目ID
     */
    public static void setTemplateCreateMDC(String projectId) {
        MDC.put("projectId", projectId);
        MDC.put("logFileName", "template_create");
    }

    /**
     * 清除MDC变量
     */
    public static void clearMDC() {
        MDC.clear();
    }
}
