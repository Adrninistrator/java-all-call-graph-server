package com.github.adrninistrator.jacgserver.model.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * javacg2配置数据传输对象
 * 
 * 注意：mainConfig和elConfig的值类型为Object，以支持前端传入的Boolean等类型，
 * 在写入配置文件时会统一转换为字符串。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class JavaCG2ConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主配置（键值对）
     * key: 配置项名称
     * value: 配置值（可能是String、Boolean等类型，写入时转换为字符串）
     */
    private Map<String, Object> mainConfig;

    /**
     * List配置（区分顺序）
     * key: 配置项名称
     * value: 配置值列表
     */
    private Map<String, List<String>> listConfig;

    /**
     * Set配置（不区分顺序）
     * key: 配置项名称
     * value: 配置值列表
     */
    private Map<String, List<String>> setConfig;

    /**
     * EL表达式配置
     * key: 配置项名称
     * value: 配置值（可能是String、Boolean等类型，写入时转换为字符串）
     */
    private Map<String, Object> elConfig;

    public Map<String, Object> getMainConfig() {
        return mainConfig;
    }

    public void setMainConfig(Map<String, Object> mainConfig) {
        this.mainConfig = mainConfig;
    }

    public Map<String, List<String>> getListConfig() {
        return listConfig;
    }

    public void setListConfig(Map<String, List<String>> listConfig) {
        this.listConfig = listConfig;
    }

    public Map<String, List<String>> getSetConfig() {
        return setConfig;
    }

    public void setSetConfig(Map<String, List<String>> setConfig) {
        this.setConfig = setConfig;
    }

    public Map<String, Object> getElConfig() {
        return elConfig;
    }

    public void setElConfig(Map<String, Object> elConfig) {
        this.elConfig = elConfig;
    }
}
