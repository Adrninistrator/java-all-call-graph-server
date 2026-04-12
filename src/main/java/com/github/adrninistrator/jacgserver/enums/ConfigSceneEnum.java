package com.github.adrninistrator.jacgserver.enums;

/**
 * 配置场景枚举
 * 用于区分项目配置和模板配置的展示规则
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public enum ConfigSceneEnum {

    /**
     * 项目配置场景
     * 用于创建/编辑项目时的配置展示
     */
    PROJECT("project", "项目配置"),

    /**
     * 模板配置场景
     * 用于创建/编辑模板时的配置展示
     */
    TEMPLATE("template", "模板配置");

    private final String code;
    private final String desc;

    ConfigSceneEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 场景代码
     * @return 枚举值，未找到返回null
     */
    public static ConfigSceneEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ConfigSceneEnum scene : values()) {
            if (scene.code.equals(code)) {
                return scene;
            }
        }
        return null;
    }
}
