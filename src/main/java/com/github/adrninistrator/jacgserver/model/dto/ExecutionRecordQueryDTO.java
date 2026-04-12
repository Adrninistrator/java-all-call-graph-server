package com.github.adrninistrator.jacgserver.model.dto;

import java.util.Date;

/**
 * 执行记录查询参数 DTO
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ExecutionRecordQueryDTO {

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 最小开始时间
     */
    private Date minStartTime;

    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页数量（10/20/30/40/50）
     */
    private Integer pageSize = 10;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Date getMinStartTime() {
        return minStartTime;
    }

    public void setMinStartTime(Date minStartTime) {
        this.minStartTime = minStartTime;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
