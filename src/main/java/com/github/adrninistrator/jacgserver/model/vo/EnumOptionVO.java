package com.github.adrninistrator.jacgserver.model.vo;

import java.io.Serializable;

/**
 * 枚举选项视图对象
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class EnumOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 枚举值
     */
    private String value;

    /**
     * 枚举描述
     */
    private String description;

    public EnumOptionVO() {
    }

    public EnumOptionVO(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
