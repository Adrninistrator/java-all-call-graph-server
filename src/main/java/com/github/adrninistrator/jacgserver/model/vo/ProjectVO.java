package com.github.adrninistrator.jacgserver.model.vo;

import com.github.adrninistrator.jacgserver.model.dto.JACGConfigDTO;
import com.github.adrninistrator.jacgserver.model.dto.JavaCG2ConfigDTO;

import java.io.Serializable;

/**
 * 项目视图对象
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ProjectVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 项目描述
     */
    private String description;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;

    /**
     * JavaCG2配置
     */
    private JavaCG2ConfigDTO javaCG2Config;

    /**
     * JACG配置
     */
    private JACGConfigDTO jacgConfig;

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

    public JavaCG2ConfigDTO getJavaCG2Config() {
        return javaCG2Config;
    }

    public void setJavaCG2Config(JavaCG2ConfigDTO javaCG2Config) {
        this.javaCG2Config = javaCG2Config;
    }

    public JACGConfigDTO getJacgConfig() {
        return jacgConfig;
    }

    public void setJacgConfig(JACGConfigDTO jacgConfig) {
        this.jacgConfig = jacgConfig;
    }
}
