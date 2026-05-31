package com.github.adrninistrator.jacgserver.model.entity;

import java.io.Serializable;

/**
 * 调用链文件信息实体类（H2数据库持久化）
 * 存储模板执行生成的调用链文件路径信息，通过record_id关联call_graph_execution_record表
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class CallGraphFileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（自增）
     */
    private Long id;

    /**
     * 关联的调用链执行记录ID（call_graph_execution_record.id）
     */
    private Long recordId;

    /**
     * 入口方法
     */
    private String entryMethod;

    /**
     * 原始文本
     */
    private String origText;

    /**
     * 生成调用链文件的完整路径
     */
    private String filePath;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getEntryMethod() {
        return entryMethod;
    }

    public void setEntryMethod(String entryMethod) {
        this.entryMethod = entryMethod;
    }

    public String getOrigText() {
        return origText;
    }

    public void setOrigText(String origText) {
        this.origText = origText;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
