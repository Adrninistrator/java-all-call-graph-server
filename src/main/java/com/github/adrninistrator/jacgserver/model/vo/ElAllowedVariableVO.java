package com.github.adrninistrator.jacgserver.model.vo;

import java.io.Serializable;
import java.util.List;

/**
 * EL表达式允许使用的变量视图对象
 *
 * 用于在query_config接口中返回EL表达式配置参数允许使用的变量信息，
 * 使AI能够理解当前表达式怎样使用。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ElAllowedVariableVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 枚举常量名称（如EAVE_PARSE_CLASS_NAME）
     */
    private String enumConstantName;

    /**
     * 变量名称，在表达式中使用的名称（如class_name）
     */
    private String variableName;

    /**
     * 变量类型（如String、int、Set等）
     */
    private String type;

    /**
     * 是否为{名称前缀}{数字}的形式
     * 当为true时，变量名需要拼接数字后使用，如class_dir_prefix_level_1、class_dir_prefix_level_2
     */
    private boolean prefixWithNum;

    /**
     * 变量描述
     */
    private List<String> descriptions;

    /**
     * 变量值示例
     */
    private List<String> valueExamples;

    public String getEnumConstantName() {
        return enumConstantName;
    }

    public void setEnumConstantName(String enumConstantName) {
        this.enumConstantName = enumConstantName;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPrefixWithNum() {
        return prefixWithNum;
    }

    public void setPrefixWithNum(boolean prefixWithNum) {
        this.prefixWithNum = prefixWithNum;
    }

    public List<String> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<String> descriptions) {
        this.descriptions = descriptions;
    }

    public List<String> getValueExamples() {
        return valueExamples;
    }

    public void setValueExamples(List<String> valueExamples) {
        this.valueExamples = valueExamples;
    }
}
