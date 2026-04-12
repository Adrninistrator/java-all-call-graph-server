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
     * JavaCG2配置
     */
    private JavaCG2ConfigDTO javaCG2Config;

    /**
     * JACG配置
     */
    private JACGConfigDTO jacgConfig;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
