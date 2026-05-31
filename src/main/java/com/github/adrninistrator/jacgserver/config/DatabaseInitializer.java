package com.github.adrninistrator.jacgserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 数据库初始化器
 * 应用启动时自动检查并创建所需的数据库表
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        logger.info("开始检查数据库表...");
        initAnalysisExecutionRecordTable();
        initCallGraphExecutionRecordTable();
        initCallGraphFileInfoTable();
        initFindStackExecutionRecordTable();
        logger.info("数据库表检查完成");
    }

    /**
     * 初始化项目静态解析执行记录表
     */
    private void initAnalysisExecutionRecordTable() {
        // 使用 IF NOT EXISTS 语法创建表（不存在时才创建）
        String createTableSql = "CREATE TABLE IF NOT EXISTS analysis_execution_record (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID'," +
                "exec_id VARCHAR(50) NOT NULL COMMENT '执行ID（毫秒级时间戳）'," +
                "project_id VARCHAR(50) NOT NULL COMMENT '项目ID'," +
                "project_desc VARCHAR(500) COMMENT '项目描述'," +
                "jar_files CLOB COMMENT '解析使用的jar文件等列表（每项之间使用\\n分隔）'," +
                "start_time TIMESTAMP NOT NULL COMMENT '开始时间'," +
                "end_time TIMESTAMP COMMENT '结束时间'," +
                "status VARCHAR(20) NOT NULL COMMENT '执行状态：running / completed / failed'," +
                "duration BIGINT COMMENT '执行耗时（毫秒）'," +
                "error_message CLOB COMMENT '错误信息（失败时）'," +
                "create_time TIMESTAMP NOT NULL COMMENT '创建时间'," +
                "update_time TIMESTAMP NOT NULL COMMENT '更新时间'" +
                ")";
        jdbcTemplate.execute(createTableSql);
        logger.info("表 analysis_execution_record 检查/创建完成");

        // 创建索引
        createIndexIfNotExists("idx_exec_id", "analysis_execution_record", "exec_id");
        createIndexIfNotExists("idx_project_id", "analysis_execution_record", "project_id");
        createIndexIfNotExists("idx_start_time", "analysis_execution_record", "start_time");
        addColumnIfNotExists("analysis_execution_record", "log_file_path", "VARCHAR(500) COMMENT '日志文件路径'");
    }

    /**
     * 初始化模板生成调用链记录表
     */
    private void initCallGraphExecutionRecordTable() {
        // 使用 IF NOT EXISTS 语法创建表（不存在时才创建）
        String createTableSql = "CREATE TABLE IF NOT EXISTS call_graph_execution_record (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID'," +
                "exec_id VARCHAR(50) NOT NULL COMMENT '执行ID（毫秒级时间戳）'," +
                "project_id VARCHAR(50) NOT NULL COMMENT '项目ID'," +
                "template_id VARCHAR(50) NOT NULL COMMENT '模板ID'," +
                "direction VARCHAR(20) NOT NULL COMMENT '调用链方向：caller / callee'," +
                "entry_methods CLOB COMMENT '入口类/方法列表（每项之间使用\\n分隔）'," +
                "start_time TIMESTAMP NOT NULL COMMENT '开始时间'," +
                "end_time TIMESTAMP COMMENT '结束时间'," +
                "status VARCHAR(20) NOT NULL COMMENT '执行状态：running / completed / failed'," +
                "duration BIGINT COMMENT '执行耗时（毫秒）'," +
                "output_dir VARCHAR(500) COMMENT '生成的调用链文件目录路径'," +
                "error_message CLOB COMMENT '错误信息（失败时）'" +
                ")";
        jdbcTemplate.execute(createTableSql);
        logger.info("表 call_graph_execution_record 检查/创建完成");

        createIndexIfNotExists("idx_cg_exec_id", "call_graph_execution_record", "exec_id");
        createIndexIfNotExists("idx_cg_template_id", "call_graph_execution_record", "template_id");
        createIndexIfNotExists("idx_cg_project_id", "call_graph_execution_record", "project_id");
        createIndexIfNotExists("idx_cg_start_time", "call_graph_execution_record", "start_time");
        addColumnIfNotExists("call_graph_execution_record", "log_file_path", "VARCHAR(500) COMMENT '日志文件路径'");
    }

    /**
     * 初始化调用链文件信息表
     * 存储模板执行生成的调用链文件路径信息，通过record_id关联call_graph_execution_record表
     */
    private void initCallGraphFileInfoTable() {
        String createTableSql = "CREATE TABLE IF NOT EXISTS call_graph_file_info (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID'," +
                "record_id BIGINT NOT NULL COMMENT '关联的调用链执行记录ID'," +
                "entry_method VARCHAR(500) NOT NULL COMMENT '入口方法'," +
                "orig_text CLOB COMMENT '原始文本'," +
                "file_path VARCHAR(1000) NOT NULL COMMENT '生成调用链文件的完整路径'" +
                ")";
        jdbcTemplate.execute(createTableSql);
        logger.info("表 call_graph_file_info 检查/创建完成");

        createIndexIfNotExists("idx_cfi_record_id", "call_graph_file_info", "record_id");
    }

    /**
     * 初始化模板生成调用链根据关键字生成堆栈记录表
     */
    private void initFindStackExecutionRecordTable() {
        // 使用 IF NOT EXISTS 语法创建表（不存在时才创建）
        String createTableSql = "CREATE TABLE IF NOT EXISTS find_stack_execution_record (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID'," +
                "exec_id VARCHAR(50) NOT NULL COMMENT '执行ID（毫秒级时间戳）'," +
                "project_id VARCHAR(50) NOT NULL COMMENT '项目ID'," +
                "template_id VARCHAR(50) NOT NULL COMMENT '模板ID'," +
                "direction VARCHAR(20) NOT NULL COMMENT '调用链方向：caller / callee'," +
                "entry_methods CLOB COMMENT '入口类/方法列表（每项之间使用\\n分隔）'," +
                "keywords CLOB COMMENT '关键字列表（每项之间使用\\n分隔）'," +
                "start_time TIMESTAMP NOT NULL COMMENT '开始时间'," +
                "end_time TIMESTAMP COMMENT '结束时间'," +
                "status VARCHAR(20) NOT NULL COMMENT '执行状态：running / completed / failed'," +
                "duration BIGINT COMMENT '执行耗时（毫秒）'," +
                "output_dir VARCHAR(500) COMMENT '生成的调用链文件目录路径'," +
                "error_message CLOB COMMENT '错误信息（失败时）'" +
                ")";
        jdbcTemplate.execute(createTableSql);
        logger.info("表 find_stack_execution_record 检查/创建完成");

        createIndexIfNotExists("idx_fs_exec_id", "find_stack_execution_record", "exec_id");
        createIndexIfNotExists("idx_fs_template_id", "find_stack_execution_record", "template_id");
        createIndexIfNotExists("idx_fs_project_id", "find_stack_execution_record", "project_id");
        createIndexIfNotExists("idx_fs_start_time", "find_stack_execution_record", "start_time");
    }

    /**
     * 创建索引（如果不存在）
     *
     * @param indexName  索引名称
     * @param tableName  表名称
     * @param columnName 列名称
     */
    private void createIndexIfNotExists(String indexName, String tableName, String columnName) {
        // 检查索引是否存在
        String checkIndexSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE INDEX_NAME = ?";
        Integer count = jdbcTemplate.queryForObject(checkIndexSql, Integer.class, indexName.toUpperCase());

        if (count == null || count == 0) {
            String createIndexSql = String.format("CREATE INDEX %s ON %s(%s)", indexName, tableName, columnName);
            jdbcTemplate.execute(createIndexSql);
            logger.info("索引 {} 创建成功", indexName);
        }
    }

    /**
     * 为已有表添加列（如果不存在）
     *
     * @param tableName  表名称
     * @param columnName 列名称
     * @param columnType 列类型
     */
    private void addColumnIfNotExists(String tableName, String columnName, String columnType) {
        try {
            // 检查列是否存在
            String checkColumnSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, tableName.toUpperCase(), columnName.toUpperCase());

            if (count == null || count == 0) {
                String alterSql = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnType);
                jdbcTemplate.execute(alterSql);
                logger.info("列 {}.{} 添加成功", tableName, columnName);
            }
        } catch (Exception e) {
            logger.warn("检查/添加列 {}.{} 时出现异常: {}", tableName, columnName, e.getMessage());
        }
    }
}
