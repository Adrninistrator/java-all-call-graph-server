package com.github.adrninistrator.jacgserver.model.vo;

import java.util.List;

/**
 * EL表达式菜单分类VO
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ElMenuCategoryVO {

    /**
     * 分类名称
     */
    private String name;

    /**
     * 菜单项列表
     */
    private List<ElMenuItemVO> items;

    public ElMenuCategoryVO() {
    }

    public ElMenuCategoryVO(String name, List<ElMenuItemVO> items) {
        this.name = name;
        this.items = items;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ElMenuItemVO> getItems() {
        return items;
    }

    public void setItems(List<ElMenuItemVO> items) {
        this.items = items;
    }
}