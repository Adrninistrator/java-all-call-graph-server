package com.github.adrninistrator.jacgserver.model.entity;

import java.io.Serializable;

/**
 * 执行记录实体类（内存中存储）
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ExecutionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 执行ID（毫秒级时间戳）
     */
    private String execId;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 模板ID（调用链生成时有值）
     */
    private String templateId;

    /**
     * 执行类型：analysis / callgraph
     */
    private String type;

    /**
     * 执行状态：running / completed / failed
     */
    private String status;

    /**
     * 开始时间戳
     */
    private long startTime;

    /**
     * 结束时间戳
     */
    private long endTime;

    /**
     * 输出目录
     */
    private String outputDir;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 日志文件路径
     */
    private String logFilePath;

    public String getExecId() {
        return execId;
    }

    public void setExecId(String execId) {
        this.execId = execId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }
}
