package com.github.adrninistrator.jacgserver.model.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 配置项视图对象
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ConfigItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置项key（枚举名称）
     */
    private String key;

    /**
     * 配置项名称
     */
    private String name;

    /**
     * 配置项类型（Boolean, String, Integer等）
     */
    private String type;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 完整描述（数组格式）
     */
    private List<String> description;

    /**
     * 简要描述（描述数组的第一个元素）
     */
    private String descriptionBrief;

    /**
     * 是否可编辑
     */
    private Boolean editable;

    /**
     * 枚举选项列表（仅当参数为枚举类型时有值）
     */
    private List<EnumOptionVO> enumOptions;

    /**
     * 参数依赖关系（有依赖时返回）
     */
    private DependsOnVO dependsOn;

    /**
     * 最小值（仅Integer类型参数有值）
     */
    private Integer minValue;

    /**
     * 最大值（仅Integer类型参数有值）
     */
    private Integer maxValue;

    /**
     * 是否必填/关键配置（true-展示在外层界面，false-在高级配置中）
     */
    private Boolean required;

    /**
     * 配置文件名称
     */
    private String fileName;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
        // 自动设置简要描述
        if (description != null && !description.isEmpty()) {
            this.descriptionBrief = description.get(0);
        }
    }

    public String getDescriptionBrief() {
        return descriptionBrief;
    }

    public void setDescriptionBrief(String descriptionBrief) {
        this.descriptionBrief = descriptionBrief;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public List<EnumOptionVO> getEnumOptions() {
        return enumOptions;
    }

    public void setEnumOptions(List<EnumOptionVO> enumOptions) {
        this.enumOptions = enumOptions;
    }

    public DependsOnVO getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(DependsOnVO dependsOn) {
        this.dependsOn = dependsOn;
    }

    public Integer getMinValue() {
        return minValue;
    }

    public void setMinValue(Integer minValue) {
        this.minValue = minValue;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
