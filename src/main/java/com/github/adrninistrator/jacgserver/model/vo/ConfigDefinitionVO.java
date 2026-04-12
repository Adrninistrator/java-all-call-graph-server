package com.github.adrninistrator.jacgserver.model.vo;

import java.io.Serializable;

/**
 * 配置定义视图对象
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ConfigDefinitionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * JavaCG2配置定义
     */
    private JavaCG2ConfigDefinitionVO javaCG2;

    /**
     * JACG配置定义
     */
    private JACGConfigDefinitionVO jacg;

    public JavaCG2ConfigDefinitionVO getJavaCG2() {
        return javaCG2;
    }

    public void setJavaCG2(JavaCG2ConfigDefinitionVO javaCG2) {
        this.javaCG2 = javaCG2;
    }

    public JACGConfigDefinitionVO getJacg() {
        return jacg;
    }

    public void setJacg(JACGConfigDefinitionVO jacg) {
        this.jacg = jacg;
    }
}
