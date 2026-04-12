-- 项目静态解析执行记录表
CREATE TABLE IF NOT EXISTS analysis_execution_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    exec_id VARCHAR(50) NOT NULL COMMENT '执行ID（毫秒级时间戳）',
    project_id VARCHAR(50) NOT NULL COMMENT '项目ID',
    project_desc VARCHAR(500) COMMENT '项目描述',
    jar_files CLOB COMMENT '解析使用的jar文件等列表（每项之间使用\n分隔）',
    start_time TIMESTAMP NOT NULL COMMENT '开始时间',
    end_time TIMESTAMP COMMENT '结束时间',
    status VARCHAR(20) NOT NULL COMMENT '执行状态：running / completed / failed',
    error_message CLOB COMMENT '错误信息（失败时）',
    create_time TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL COMMENT '更新时间'
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_exec_id ON analysis_execution_record(exec_id);
CREATE INDEX IF NOT EXISTS idx_project_id ON analysis_execution_record(project_id);
CREATE INDEX IF NOT EXISTS idx_start_time ON analysis_execution_record(start_time);
