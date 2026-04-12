package com.github.adrninistrator.jacgserver.model.vo;

import java.io.Serializable;

/**
 * 参数依赖关系视图对象
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class DependsOnVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 依赖的参数key
     */
    private String paramKey;

    /**
     * 依赖参数的期望值
     */
    private String paramValue;

    /**
     * 当条件满足时的动作：enable（启用）、disable（禁用）、show（显示）、hide（隐藏）
     */
    private String action;

    public DependsOnVO() {
    }

    public DependsOnVO(String paramKey, String paramValue, String action) {
        this.paramKey = paramKey;
        this.paramValue = paramValue;
        this.action = action;
    }

    public String getParamKey() {
        return paramKey;
    }

    public void setParamKey(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getParamValue() {
        return paramValue;
    }

    public void setParamValue(String paramValue) {
        this.paramValue = paramValue;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
