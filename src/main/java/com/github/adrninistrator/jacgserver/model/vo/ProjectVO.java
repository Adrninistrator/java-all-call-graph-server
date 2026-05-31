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
     * 项目根目录（被解析的代码对应项目的根目录）
     */
    private String projectRootDir;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;

    /**
     * 是否通过MCP创建
     */
    private boolean createdByMcp;

    /**
     * javacg2配置
     */
    private JavaCG2ConfigDTO javacg2Config;

    /**
     * jacg配置
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

    public JavaCG2ConfigDTO getJavacg2Config() {
        return javacg2Config;
    }

    public void setJavacg2Config(JavaCG2ConfigDTO javacg2Config) {
        this.javacg2Config = javacg2Config;
    }

    public JACGConfigDTO getJacgConfig() {
        return jacgConfig;
    }

    public void setJacgConfig(JACGConfigDTO jacgConfig) {
        this.jacgConfig = jacgConfig;
    }

    public String getProjectRootDir() {
        return projectRootDir;
    }

    public void setProjectRootDir(String projectRootDir) {
        this.projectRootDir = projectRootDir;
    }

    public boolean isCreatedByMcp() {
        return createdByMcp;
    }

    public void setCreatedByMcp(boolean createdByMcp) {
        this.createdByMcp = createdByMcp;
    }
}
