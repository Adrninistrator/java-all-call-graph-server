package com.github.adrninistrator.jacgserver.model.vo;

import java.util.Date;
import java.util.List;

/**
 * 执行记录列表返回 VO（带分页信息）
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ExecutionRecordVO {

    /**
     * 执行记录列表
     */
    private List<RecordItem> records;

    /**
     * 总记录数
     */
    private Integer total;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页数量
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer totalPages;

    public List<RecordItem> getRecords() {
        return records;
    }

    public void setRecords(List<RecordItem> records) {
        this.records = records;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
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

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    /**
     * 执行记录项
     */
    public static class RecordItem {

        private Long id;
        private String execId;
        private String projectId;
        private String projectDesc;
        private String jarFiles;
        private Date startTime;
        private Date endTime;
        private String status;
        private Long duration;
        private String errorMessage;

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

        public String getProjectDesc() {
            return projectDesc;
        }

        public void setProjectDesc(String projectDesc) {
            this.projectDesc = projectDesc;
        }

        public String getJarFiles() {
            return jarFiles;
        }

        public void setJarFiles(String jarFiles) {
            this.jarFiles = jarFiles;
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

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
