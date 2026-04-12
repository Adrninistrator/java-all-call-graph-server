package com.github.adrninistrator.jacgserver.model.vo;

/**
 * EL表达式菜单项VO
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ElMenuItemVO {

    /**
     * 菜单项名称
     */
    private String name;

    /**
     * 展示文本
     */
    private String displayText;

    /**
     * 插入文本
     */
    private String insertText;

    public ElMenuItemVO() {
    }

    public ElMenuItemVO(String name, String displayText, String insertText) {
        this.name = name;
        this.displayText = displayText;
        this.insertText = insertText;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getInsertText() {
        return insertText;
    }

    public void setInsertText(String insertText) {
        this.insertText = insertText;
    }
}