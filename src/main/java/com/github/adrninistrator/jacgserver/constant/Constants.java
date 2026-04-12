package com.github.adrninistrator.jacgserver.constant;

/**
 * 常量定义
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public final class Constants {

    private Constants() {
    }

    /**
     * 输出根目录JVM参数名
     */
    public static final String JVM_OUTPUT_ROOT_PATH = "jacgserver.output.root.path";

    /**
     * 项目配置目录名
     */
    public static final String PROJECT_CONF_DIR = "project_conf";

    /**
     * 项目列表配置文件名
     */
    public static final String PROJECT_JSON_FILE = "project.json";

    /**
     * 模板目录名
     */
    public static final String TEMPLATES_DIR = "templates";

    /**
     * 执行类型 - 静态分析
     */
    public static final String EXEC_TYPE_ANALYSIS = "analysis";

    /**
     * 执行类型 - 生成调用链
     */
    public static final String EXEC_TYPE_CALLGRAPH = "callgraph";

    /**
     * 执行类型 - 生成调用链根据关键字生成堆栈
     */
    public static final String EXEC_TYPE_FINDSTACK = "findstack";

    /**
     * 执行状态 - 执行中
     */
    public static final String EXEC_STATUS_RUNNING = "running";

    /**
     * 执行状态 - 已完成
     */
    public static final String EXEC_STATUS_COMPLETED = "completed";

    /**
     * 执行状态 - 失败
     */
    public static final String EXEC_STATUS_FAILED = "failed";

    /**
     * 调用链方向 - 向下调用链
     */
    public static final String DIRECTION_CALLER = "caller";

    /**
     * 调用链方向 - 向上调用链
     */
    public static final String DIRECTION_CALLEE = "callee";

    /**
     * 默认日期时间格式
     */
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 内存执行记录最大数量
     */
    public static final int MAX_EXECUTION_RECORDS = 100;

    /**
     * 日志目录
     */
    public static final String LOG_DIR = "./log";
}
