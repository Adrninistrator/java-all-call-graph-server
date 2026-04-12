package com.github.adrninistrator.jacgserver.config;

import com.adrninistrator.jacg.conf.enums.ConfigDbKeyEnum;
import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseListEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseSetEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 配置可见性定义
 * 定义哪些配置默认展示在外层界面，哪些配置隐藏在高级配置中
 *
 * 设计说明：
 * - 默认展示的配置：项目/模板创建和编辑时必须或常用的配置
 * - 隐藏的配置：高级配置，用户需要点击"高级配置"按钮才能看到
 * - 模板配置的场景规则：visible=false表示不展示，editable=false表示只读
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ConfigVisibilityDefinition {

    /**
     * JavaCG2主配置：默认展示的配置项（项目配置）
     * 这些配置在外层界面直接显示
     */
    private static final Set<String> VISIBLE_JAVACG2_MAIN_CONFIG = new HashSet<>(Arrays.asList(
            // JavaCG2主配置目前没有特别需要在外层展示的
    ));

    /**
     * JavaCG2 List配置：默认展示的配置项（项目配置）
     * 这些配置在外层界面直接显示
     */
    private static final Set<String> VISIBLE_JAVACG2_LIST_CONFIG = new HashSet<>(Arrays.asList(
            // Jar/Class文件路径，项目必须配置
            JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR.name()
    ));

    /**
     * JACG主配置：默认展示的配置项（项目配置）
     * 这些配置在外层界面直接显示
     */
    private static final Set<String> VISIBLE_JACG_MAIN_CONFIG = new HashSet<>(Arrays.asList(
            // 目前没有特别需要在外层展示的JACG主配置
    ));

    /**
     * JACG数据库配置：默认展示的配置项（项目配置）
     * 这些配置在外层界面直接显示
     */
    private static final Set<String> VISIBLE_JACG_DB_CONFIG = new HashSet<>(Arrays.asList(
            // 是否使用H2数据库
            ConfigDbKeyEnum.CDKE_DB_USE_H2.name()
    ));

    /**
     * JACG Set配置：默认展示的配置项（项目配置）
     * 这些配置在外层界面直接显示
     */
    private static final Set<String> VISIBLE_JACG_SET_CONFIG = new HashSet<>(Arrays.asList(
            // 向下调用链入口类/方法
            OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLER.name(),
            // 向上调用链入口类/方法
            OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLEE.name()
    ));

    // ==================== 模板配置场景规则 ====================

    /**
     * 模板配置：不展示的JACG主配置项
     * 这些配置在模板中不需要处理
     */
    public static final Set<String> TEMPLATE_EXCLUDED_JACG_MAIN_CONFIG = new HashSet<>(Arrays.asList(
            ConfigKeyEnum.CKE_DB_INSERT_BATCH_SIZE.name(),
            ConfigKeyEnum.CKE_DROP_OR_TRUNCATE_TABLE.name(),
            ConfigKeyEnum.CKE_SKIP_WRITE_DB_WHEN_JAR_NOT_MODIFIED.name(),
            ConfigKeyEnum.CKE_PARSE_SPRING_AOP_INFO.name()
    ));

    /**
     * 模板配置：只读的JACG主配置项（展示但不允许修改，使用项目中对应的值）
     */
    public static final Set<String> TEMPLATE_READONLY_JACG_MAIN_CONFIG = new HashSet<>(Arrays.asList(
            ConfigKeyEnum.CKE_APP_NAME.name()
    ));

    /**
     * 模板配置：不展示的JACG List配置项
     * 这些配置在模板中不需要处理
     */
    public static final Set<String> TEMPLATE_EXCLUDED_JACG_LIST_CONFIG = new HashSet<>(Arrays.asList(
            OtherConfigFileUseListEnum.OCFULE_EXTENSIONS_CODE_PARSER.name(),
            OtherConfigFileUseListEnum.OCFULE_EXTENSIONS_MANUAL_ADD_METHOD_CALL1.name(),
            OtherConfigFileUseListEnum.OCFULE_EXTENSIONS_JAVACG2_METHOD_CALL.name(),
            OtherConfigFileUseListEnum.OCFULE_EXTENSIONS_JACG_METHOD_CALL.name()
    ));

    /**
     * 判断JavaCG2主配置项是否默认展示
     *
     * @param configKey 配置项key
     * @return true-默认展示，false-隐藏
     */
    public static boolean isJavaCG2MainConfigVisible(JavaCG2ConfigKeyEnum configKey) {
        return VISIBLE_JAVACG2_MAIN_CONFIG.contains(configKey.name());
    }

    /**
     * 判断JavaCG2 List配置项是否默认展示
     *
     * @param configKey 配置项key
     * @return true-默认展示，false-隐藏
     */
    public static boolean isJavaCG2ListConfigVisible(JavaCG2OtherConfigFileUseListEnum configKey) {
        return VISIBLE_JAVACG2_LIST_CONFIG.contains(configKey.name());
    }

    /**
     * 判断JACG主配置项是否默认展示
     *
     * @param configKey 配置项key
     * @return true-默认展示，false-隐藏
     */
    public static boolean isJacgMainConfigVisible(ConfigKeyEnum configKey) {
        return VISIBLE_JACG_MAIN_CONFIG.contains(configKey.name());
    }

    /**
     * 判断JACG数据库配置项是否默认展示
     *
     * @param configKey 配置项key
     * @return true-默认展示，false-隐藏
     */
    public static boolean isJacgDbConfigVisible(ConfigDbKeyEnum configKey) {
        return VISIBLE_JACG_DB_CONFIG.contains(configKey.name());
    }

    /**
     * 判断JACG List配置项是否默认展示
     * JACG List配置默认不展示在项目场景
     *
     * @param configKey 配置项key
     * @return true-默认展示，false-隐藏
     */
    public static boolean isJacgListConfigVisible(OtherConfigFileUseListEnum configKey) {
        // JACG List配置默认不展示
        return false;
    }

    /**
     * 判断JACG Set配置项是否默认展示
     *
     * @param configKey 配置项key
     * @return true-默认展示，false-隐藏
     */
    public static boolean isJacgSetConfigVisible(OtherConfigFileUseSetEnum configKey) {
        return VISIBLE_JACG_SET_CONFIG.contains(configKey.name());
    }

    /**
     * 获取默认展示的JavaCG2主配置项集合
     *
     * @return 配置项key集合
     */
    public static Set<String> getVisibleJavaCG2MainConfig() {
        return VISIBLE_JAVACG2_MAIN_CONFIG;
    }

    /**
     * 获取默认展示的JavaCG2 List配置项集合
     *
     * @return 配置项key集合
     */
    public static Set<String> getVisibleJavaCG2ListConfig() {
        return VISIBLE_JAVACG2_LIST_CONFIG;
    }

    /**
     * 获取默认展示的JACG主配置项集合
     *
     * @return 配置项key集合
     */
    public static Set<String> getVisibleJacgMainConfig() {
        return VISIBLE_JACG_MAIN_CONFIG;
    }

    /**
     * 获取默认展示的JACG数据库配置项集合
     *
     * @return 配置项key集合
     */
    public static Set<String> getVisibleJacgDbConfig() {
        return VISIBLE_JACG_DB_CONFIG;
    }

    /**
     * 获取默认展示的JACG Set配置项集合
     *
     * @return 配置项key集合
     */
    public static Set<String> getVisibleJacgSetConfig() {
        return VISIBLE_JACG_SET_CONFIG;
    }

    // ==================== 模板配置场景判断方法 ====================

    /**
     * 判断模板场景下JACG主配置项是否展示
     *
     * @param configKey 配置项key
     * @return true-展示，false-不展示
     */
    public static boolean isTemplateJacgMainConfigVisible(ConfigKeyEnum configKey) {
        return !TEMPLATE_EXCLUDED_JACG_MAIN_CONFIG.contains(configKey.name());
    }

    /**
     * 判断模板场景下JACG主配置项是否只读
     *
     * @param configKey 配置项key
     * @return true-只读，false-可编辑
     */
    public static boolean isTemplateJacgMainConfigReadonly(ConfigKeyEnum configKey) {
        return TEMPLATE_READONLY_JACG_MAIN_CONFIG.contains(configKey.name());
    }

    /**
     * 判断模板场景下JACG List配置项是否展示
     *
     * @param configKey 配置项key
     * @return true-展示，false-不展示
     */
    public static boolean isTemplateJacgListConfigVisible(OtherConfigFileUseListEnum configKey) {
        return !TEMPLATE_EXCLUDED_JACG_LIST_CONFIG.contains(configKey.name());
    }

    /**
     * 判断模板场景下JACG数据库配置项是否展示
     * 模板场景下数据库配置全部展示
     *
     * @param configKey 配置项key
     * @return true-展示，false-不展示
     */
    public static boolean isTemplateJacgDbConfigVisible(ConfigDbKeyEnum configKey) {
        // 模板场景下，数据库配置全部展示
        return true;
    }

    private ConfigVisibilityDefinition() {
        // 私有构造函数，防止实例化
    }
}
