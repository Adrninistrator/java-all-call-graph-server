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
     * javacg2配置定义
     */
    private JavaCG2ConfigDefinitionVO javacg2;

    /**
     * jacg配置定义
     */
    private JACGConfigDefinitionVO jacg;

    public JavaCG2ConfigDefinitionVO getJavacg2() {
        return javacg2;
    }

    public void setJavacg2(JavaCG2ConfigDefinitionVO javacg2) {
        this.javacg2 = javacg2;
    }

    public JACGConfigDefinitionVO getJacg() {
        return jacg;
    }

    public void setJacg(JACGConfigDefinitionVO jacg) {
        this.jacg = jacg;
    }
}
