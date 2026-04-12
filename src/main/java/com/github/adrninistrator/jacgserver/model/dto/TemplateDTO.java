package com.github.adrninistrator.jacgserver.model.dto;

import java.io.Serializable;

/**
 * 模板数据传输对象
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class TemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 调用链方向：caller(向下), callee(向上)
     */
    private String direction;

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

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public JACGConfigDTO getJacgConfig() {
        return jacgConfig;
    }

    public void setJacgConfig(JACGConfigDTO jacgConfig) {
        this.jacgConfig = jacgConfig;
    }
}
