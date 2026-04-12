package com.github.adrninistrator.jacgserver.model.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 其他配置项视图对象（List/Set类型配置）
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class OtherConfigItemVO implements Serializable {

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
     * 配置文件名
     */
    private String fileName;

    /**
     * 完整描述（数组格式）
     */
    private List<String> description;

    /**
     * 简要描述（描述数组的第一个元素）
     */
    private String descriptionBrief;

    /**
     * 是否默认展示在外层界面
     * true-默认展示，false-隐藏在高级配置中
     */
    private Boolean visible;

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
