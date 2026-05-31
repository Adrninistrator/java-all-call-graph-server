package com.github.adrninistrator.jacgserver.model.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 模板生成调用链记录实体类（H2数据库持久化）
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class CallGraphExecutionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（自增）
     */
    private Long id;

    /**
     * 执行ID（毫秒级时间戳）
     */
    private String execId;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 模板ID
     */
    private String templateId;

    /**
     * 调用链方向：caller / callee
     */
    private String direction;

    /**
     * 入口类/方法列表（每项之间使用\n分隔）
     */
    private String entryMethods;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 执行状态：running / completed / failed
     */
    private String status;

    /**
     * 执行耗时（毫秒）
     */
    private Long duration;

    /**
     * 生成的调用链文件目录路径
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getEntryMethods() {
        return entryMethods;
    }

    public void setEntryMethods(String entryMethods) {
        this.entryMethods = entryMethods;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
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