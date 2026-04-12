package com.github.adrninistrator.jacgserver.model.vo;

import java.io.Serializable;

/**
 * 表达式配置检查错误信息VO
 * 
 * 用于前端展示表达式配置检查失败的详细信息
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ElConfigCheckErrorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置来源（java-callgraph2 或 java-all-call-graph）
     */
    private String configSource;

    /**
     * 表达式配置枚举名称
     */
    private String elConfigEnumName;

    /**
     * 配置文件名
     */
    private String configFileName;

    /**
     * 配置描述
     */
    private String configDescription;

    /**
     * 表达式字符串
     */
    private String elText;

    /**
     * 错误信息（异常的getMessage返回值）
     */
    private String errorMessage;

    public ElConfigCheckErrorVO() {
    }

    public ElConfigCheckErrorVO(String configSource, String elConfigEnumName, String configFileName, 
            String configDescription, String elText, String errorMessage) {
        this.configSource = configSource;
        this.elConfigEnumName = elConfigEnumName;
        this.configFileName = configFileName;
        this.configDescription = configDescription;
        this.elText = elText;
        this.errorMessage = errorMessage;
    }

    public String getConfigSource() {
        return configSource;
    }

    public void setConfigSource(String configSource) {
        this.configSource = configSource;
    }

    public String getElConfigEnumName() {
        return elConfigEnumName;
    }

    public void setElConfigEnumName(String elConfigEnumName) {
        this.elConfigEnumName = elConfigEnumName;
    }

    public String getConfigFileName() {
        return configFileName;
    }

    public void setConfigFileName(String configFileName) {
        this.configFileName = configFileName;
    }

    public String getConfigDescription() {
        return configDescription;
    }

    public void setConfigDescription(String configDescription) {
        this.configDescription = configDescription;
    }

    public String getElText() {
        return elText;
    }

    public void setElText(String elText) {
        this.elText = elText;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
