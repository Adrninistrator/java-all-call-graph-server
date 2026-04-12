package com.github.adrninistrator.jacgserver.model.vo;

import com.github.adrninistrator.jacgserver.model.dto.JACGConfigDTO;

import java.io.Serializable;

/**
 * 模板视图对象
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class TemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    private String templateId;

    /**
     * 所属项目ID
     */
    private String projectId;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 调用链方向
     */
    private String direction;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;

    /**
     * JACG配置
     */
    private JACGConfigDTO jacgConfig;

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public JACGConfigDTO getJacgConfig() {
        return jacgConfig;
    }

    public void setJacgConfig(JACGConfigDTO jacgConfig) {
        this.jacgConfig = jacgConfig;
    }
}
