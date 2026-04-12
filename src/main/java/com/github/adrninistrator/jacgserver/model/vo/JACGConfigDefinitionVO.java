package com.github.adrninistrator.jacgserver.model.vo;

import java.io.Serializable;
import java.util.List;

/**
 * JACG配置定义视图对象
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class JACGConfigDefinitionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主配置项列表
     */
    private List<ConfigItemVO> mainConfig;

    /**
     * 数据库配置项列表
     */
    private List<ConfigItemVO> dbConfig;

    /**
     * List配置项列表（区分顺序）
     */
    private List<OtherConfigItemVO> listConfig;

    /**
     * Set配置项列表（不区分顺序）
     */
    private List<OtherConfigItemVO> setConfig;

    /**
     * EL表达式配置项列表
     */
    private List<OtherConfigItemVO> elConfig;

    public List<ConfigItemVO> getMainConfig() {
        return mainConfig;
    }

    public void setMainConfig(List<ConfigItemVO> mainConfig) {
        this.mainConfig = mainConfig;
    }

    public List<ConfigItemVO> getDbConfig() {
        return dbConfig;
    }

    public void setDbConfig(List<ConfigItemVO> dbConfig) {
        this.dbConfig = dbConfig;
    }

    public List<OtherConfigItemVO> getListConfig() {
        return listConfig;
    }

    public void setListConfig(List<OtherConfigItemVO> listConfig) {
        this.listConfig = listConfig;
    }

    public List<OtherConfigItemVO> getSetConfig() {
        return setConfig;
    }

    public void setSetConfig(List<OtherConfigItemVO> setConfig) {
        this.setConfig = setConfig;
    }

    public List<OtherConfigItemVO> getElConfig() {
        return elConfig;
    }

    public void setElConfig(List<OtherConfigItemVO> elConfig) {
        this.elConfig = elConfig;
    }
}
