package com.github.adrninistrator.jacgserver.model.dto;

import java.io.Serializable;

/**
 * 项目数据传输对象
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ProjectDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目描述
     */
    private String description;

    /**
     * 项目根目录（被解析的代码对应项目的根目录，非必填）
     */
    private String projectRootDir;

    /**
     * 是否通过MCP创建（内部使用，前端不需要设置）
     */
    private Boolean createdByMcp;

    /**
     * javacg2配置
     */
    private JavaCG2ConfigDTO javacg2Config;

    /**
     * jacg配置
     */
    private JACGConfigDTO jacgConfig;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Boolean getCreatedByMcp() {
        return createdByMcp;
    }

    public void setCreatedByMcp(Boolean createdByMcp) {
        this.createdByMcp = createdByMcp;
    }
}
