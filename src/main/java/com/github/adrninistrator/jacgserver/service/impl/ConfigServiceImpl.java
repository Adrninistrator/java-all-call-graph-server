package com.github.adrninistrator.jacgserver.service.impl;

import com.adrninistrator.jacg.common.enums.OutputDetailEnum;
import com.adrninistrator.jacg.conf.enums.ConfigDbKeyEnum;
import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseListEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseSetEnum;
import com.adrninistrator.javacg2.common.enums.JavaCG2CalleeRawActualEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseSetEnum;
import com.adrninistrator.javacg2.el.enums.interfaces.ElAllowedVariableInterface;
import com.github.adrninistrator.jacgserver.config.ConfigVisibilityDefinition;
import com.github.adrninistrator.jacgserver.constant.ComponentEnum;
import com.github.adrninistrator.jacgserver.constant.Constants;
import com.github.adrninistrator.jacgserver.enums.ConfigSceneEnum;
import com.github.adrninistrator.jacgserver.model.vo.ConfigDefinitionVO;
import com.github.adrninistrator.jacgserver.model.vo.ConfigItemVO;
import com.github.adrninistrator.jacgserver.model.vo.DependsOnVO;
import com.github.adrninistrator.jacgserver.model.vo.ElAllowedVariableVO;
import com.github.adrninistrator.jacgserver.model.vo.ElMenuCategoryVO;
import com.github.adrninistrator.jacgserver.model.vo.ElMenuItemVO;
import com.github.adrninistrator.jacgserver.model.vo.EnumOptionVO;
import com.github.adrninistrator.jacgserver.model.vo.JACGConfigDefinitionVO;
import com.github.adrninistrator.jacgserver.model.vo.JavaCG2ConfigDefinitionVO;
import com.github.adrninistrator.jacgserver.model.vo.OtherConfigItemVO;
import com.github.adrninistrator.jacgserver.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置服务实现类
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Service
public class ConfigServiceImpl implements ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigServiceImpl.class);



    @Value("${jacgserver.output.root.path:./}")
    private String outputRootPath;

    @Override
    public ConfigDefinitionVO getConfigDefinitions() {
        return getConfigDefinitions(ConfigSceneEnum.PROJECT);
    }

    @Override
    public ConfigDefinitionVO getConfigDefinitions(ConfigSceneEnum scene) {
        return getConfigDefinitions(scene, false);
    }

    @Override
    public ConfigDefinitionVO getConfigDefinitions(ConfigSceneEnum scene, boolean ignoreVisibility) {
        ConfigDefinitionVO definitions = new ConfigDefinitionVO();

        // 根据场景决定是否处理JavaCG2配置
        if (scene == ConfigSceneEnum.PROJECT) {
            // 项目配置场景：处理JavaCG2配置
            definitions.setJavacg2(buildJavaCG2ConfigDefinitions());
        } else {
            // 模板配置场景：不处理JavaCG2配置
            definitions.setJavacg2(buildEmptyJavaCG2ConfigDefinitions());
        }

        // JACG配置（项目配置和模板配置都需要处理）
        definitions.setJacg(buildJacgConfigDefinitions(scene, ignoreVisibility));

        return definitions;
    }

    @Override
    public String getOutputRootPath() {
        // 优先使用JVM参数
        String jvmPath = System.getProperty(Constants.JVM_OUTPUT_ROOT_PATH);
        if (jvmPath != null && !jvmPath.isEmpty()) {
            return jvmPath;
        }
        return outputRootPath;
    }

    @Override
    public String getProjectConfDir() {
        String rootPath = getOutputRootPath();
        // 使用File类规范化路径，避免手动处理分隔符产生双斜杠
        File rootFile = new File(rootPath);
        File projectConfDir = new File(rootFile, Constants.PROJECT_CONF_DIR);
        return projectConfDir.getPath();
    }

    /**
     * 构建空的JavaCG2配置定义（模板场景使用）
     */
    private JavaCG2ConfigDefinitionVO buildEmptyJavaCG2ConfigDefinitions() {
        JavaCG2ConfigDefinitionVO javacg2Defs = new JavaCG2ConfigDefinitionVO();
        javacg2Defs.setMainConfig(new ArrayList<>());
        javacg2Defs.setListConfig(new ArrayList<>());
        javacg2Defs.setSetConfig(new ArrayList<>());
        javacg2Defs.setElConfig(new ArrayList<>());
        return javacg2Defs;
    }

    /**
     * 构建javacg2配置定义
     */
    private JavaCG2ConfigDefinitionVO buildJavaCG2ConfigDefinitions() {
        JavaCG2ConfigDefinitionVO javacg2Defs = new JavaCG2ConfigDefinitionVO();

        // 主配置
        List<ConfigItemVO> mainConfig = new ArrayList<>();
        for (JavaCG2ConfigKeyEnum configKey : JavaCG2ConfigKeyEnum.values()) {
            ConfigItemVO config = new ConfigItemVO();
            config.setKey(configKey.name());
            config.setName(configKey.getKey());
            config.setType(configKey.getType().getSimpleName());
            config.setDefaultValue(configKey.getDefaultValue());
            config.setDescription(Arrays.asList(configKey.getDescriptions()));
            // 设置配置文件名称
            config.setFileName(configKey.getFileName());
            // OUTPUT_ROOT_PATH禁止编辑
            config.setEditable(configKey != JavaCG2ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH);

            // 标记关键必填配置（展示在外层界面）
            config.setRequired(isJavaCG2RequiredConfig(configKey));

            // 设置枚举选项
            setEnumOptions(config, configKey);

            // 设置Integer类型参数的范围限制
            setIntegerRange(config);

            mainConfig.add(config);
        }
        javacg2Defs.setMainConfig(mainConfig);

        // List配置
        List<OtherConfigItemVO> listConfig = new ArrayList<>();
        for (JavaCG2OtherConfigFileUseListEnum configEnum : JavaCG2OtherConfigFileUseListEnum.values()) {
            OtherConfigItemVO config = new OtherConfigItemVO();
            config.setKey(configEnum.name());
            // 参数名称使用配置文件名，而不是枚举名称
            config.setName(configEnum.getKey());
            config.setFileName(configEnum.getKey());
            config.setDescription(Arrays.asList(configEnum.getDescriptions()));
            config.setVisible(ConfigVisibilityDefinition.isJavaCG2ListConfigVisible(configEnum));
            listConfig.add(config);
        }
        javacg2Defs.setListConfig(listConfig);

        // Set配置
        List<OtherConfigItemVO> setConfig = new ArrayList<>();
        for (JavaCG2OtherConfigFileUseSetEnum configEnum : JavaCG2OtherConfigFileUseSetEnum.values()) {
            OtherConfigItemVO config = new OtherConfigItemVO();
            config.setKey(configEnum.name());
            // 参数名称使用配置文件名，而不是枚举名称
            config.setName(configEnum.getKey());
            config.setFileName(configEnum.getKey());
            config.setDescription(Arrays.asList(configEnum.getDescriptions()));
            setConfig.add(config);
        }
        javacg2Defs.setSetConfig(setConfig);

        // EL表达式配置
        List<OtherConfigItemVO> elConfig = new ArrayList<>();
        for (com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum configEnum : com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum.values()) {
            // 跳过示例配置
            if (configEnum == com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum.ECE_EXAMPLE) {
                continue;
            }
            OtherConfigItemVO config = new OtherConfigItemVO();
            config.setKey(configEnum.name());
            // 参数名称使用配置文件名，而不是枚举名称
            config.setName(configEnum.getKey());
            config.setFileName(configEnum.getKey());
            config.setDescription(Arrays.asList(configEnum.getDescriptions()));
            // 设置EL表达式允许使用的变量
            config.setElAllowedVariables(buildElAllowedVariables(configEnum.getElAllowedVariableEnums()));
            elConfig.add(config);
        }
        javacg2Defs.setElConfig(elConfig);

        return javacg2Defs;
    }

    /**
     * 设置枚举选项
     */
    private void setEnumOptions(ConfigItemVO config, JavaCG2ConfigKeyEnum configKey) {
        List<EnumOptionVO> enumOptions = null;

        // 根据配置项key判断是否为枚举类型
        switch (configKey) {
            case CKE_HANDLE_CALLEE_NEW_RAW_ACTUAL:
            case CKE_HANDLE_CALLEE_SPRING_BEAN_RAW_ACTUAL:
                enumOptions = buildCalleeRawActualEnumOptions();
                break;
            default:
                break;
        }

        config.setEnumOptions(enumOptions);
    }

    /**
     * 构建CalleeRawActualEnum选项
     */
    private List<EnumOptionVO> buildCalleeRawActualEnumOptions() {
        List<EnumOptionVO> options = new ArrayList<>();
        for (JavaCG2CalleeRawActualEnum enumValue : JavaCG2CalleeRawActualEnum.values()) {
            options.add(new EnumOptionVO(enumValue.getType(), enumValue.getDesc()));
        }
        return options;
    }

    /**
     * 构建JACG配置定义
     */
    private JACGConfigDefinitionVO buildJacgConfigDefinitions(ConfigSceneEnum scene, boolean ignoreVisibility) {
        JACGConfigDefinitionVO jacgDefs = new JACGConfigDefinitionVO();

        // 主配置
        List<ConfigItemVO> mainConfig = new ArrayList<>();
        for (ConfigKeyEnum configKey : ConfigKeyEnum.values()) {
            // 模板场景下，跳过不展示的配置项（忽略可见性时不跳过）
            if (!ignoreVisibility && scene == ConfigSceneEnum.TEMPLATE && !ConfigVisibilityDefinition.isTemplateJacgMainConfigVisible(configKey)) {
                continue;
            }

            ConfigItemVO config = new ConfigItemVO();
            config.setKey(configKey.name());
            config.setName(configKey.getKey());
            config.setType(configKey.getType().getSimpleName());
            config.setDefaultValue(configKey.getDefaultValue());
            config.setDescription(Arrays.asList(configKey.getDescriptions()));
            // 设置配置文件名称
            config.setFileName(configKey.getFileName());
            
            // 设置可编辑性
            boolean editable;
            if (scene == ConfigSceneEnum.TEMPLATE) {
                // 模板场景：检查是否只读
                if (ConfigVisibilityDefinition.isTemplateJacgMainConfigReadonly(configKey)) {
                    editable = false;
                } else {
                    // 其他配置项按照项目配置的规则
                    editable = isJacgMainConfigEditable(configKey);
                }
            } else {
                // 项目配置场景
                editable = isJacgMainConfigEditable(configKey);
            }
            config.setEditable(editable);

            // 标记关键必填配置（展示在外层界面）
            config.setRequired(isJacgRequiredConfig(configKey));

            // 设置枚举选项
            setEnumOptions(config, configKey);

            // 设置Integer类型参数的范围限制
            setIntegerRange(config);

            mainConfig.add(config);
        }
        jacgDefs.setMainConfig(mainConfig);

        // 数据库配置（项目配置和模板配置都需要处理）
        List<ConfigItemVO> dbConfig = new ArrayList<>();
        for (ConfigDbKeyEnum configKey : ConfigDbKeyEnum.values()) {
            // 模板场景下，检查数据库配置是否需要展示（忽略可见性时不跳过）
            if (!ignoreVisibility && scene == ConfigSceneEnum.TEMPLATE && !ConfigVisibilityDefinition.isTemplateJacgDbConfigVisible(configKey)) {
                continue;
            }

            ConfigItemVO config = new ConfigItemVO();
            config.setKey(configKey.name());
            config.setName(configKey.getKey());
            config.setType(configKey.getType().getSimpleName());
            config.setDefaultValue(configKey.getDefaultValue());
            config.setDescription(Arrays.asList(configKey.getDescriptions()));
            // 设置配置文件名称
            config.setFileName(configKey.getFileName());
            // 模板场景下，数据库配置不允许人工编辑（自动使用项目的数据库配置）
            config.setEditable(scene != ConfigSceneEnum.TEMPLATE);

            // 标记关键必填配置（展示在外层界面）
            config.setRequired(isDbRequiredConfig(configKey));

            // 设置数据库配置参数的依赖关系
            setDbConfigDependency(config, configKey);

            dbConfig.add(config);
        }
        jacgDefs.setDbConfig(dbConfig);

        // List配置
        List<OtherConfigItemVO> listConfig = new ArrayList<>();
        for (OtherConfigFileUseListEnum configEnum : OtherConfigFileUseListEnum.values()) {
            // 模板场景下，跳过不展示的配置项（忽略可见性时不跳过）
            if (!ignoreVisibility && scene == ConfigSceneEnum.TEMPLATE && !ConfigVisibilityDefinition.isTemplateJacgListConfigVisible(configEnum)) {
                continue;
            }

            OtherConfigItemVO config = new OtherConfigItemVO();
            config.setKey(configEnum.name());
            // 参数名称使用配置文件名，而不是枚举名称
            config.setName(configEnum.getKey());
            config.setFileName(configEnum.getKey());
            config.setDescription(Arrays.asList(configEnum.getDescriptions()));
            // List配置默认不展示在项目场景，模板场景已经过滤了不展示的
            config.setVisible(scene == ConfigSceneEnum.TEMPLATE || ConfigVisibilityDefinition.isJacgListConfigVisible(configEnum));
            listConfig.add(config);
        }
        jacgDefs.setListConfig(listConfig);

        // Set配置
        List<OtherConfigItemVO> setConfig = new ArrayList<>();
        for (OtherConfigFileUseSetEnum configEnum : OtherConfigFileUseSetEnum.values()) {
            OtherConfigItemVO config = new OtherConfigItemVO();
            config.setKey(configEnum.name());
            // 参数名称使用配置文件名，而不是枚举名称
            config.setName(configEnum.getKey());
            config.setFileName(configEnum.getKey());
            config.setDescription(Arrays.asList(configEnum.getDescriptions()));
            config.setVisible(ConfigVisibilityDefinition.isJacgSetConfigVisible(configEnum));
            // 模板场景下，入口类/方法配置在默认模板中不可编辑
            if (scene == ConfigSceneEnum.TEMPLATE
                    && (configEnum == OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLEE
                    || configEnum == OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLER)) {
                config.setEditable(false);
            }
            setConfig.add(config);
        }
        jacgDefs.setSetConfig(setConfig);

        // EL表达式配置（项目配置和模板配置都需要处理）
        List<OtherConfigItemVO> elConfig = new ArrayList<>();
        for (com.adrninistrator.jacg.el.enums.ElConfigEnum configEnum : com.adrninistrator.jacg.el.enums.ElConfigEnum.values()) {
            // 跳过示例配置
            if (configEnum == com.adrninistrator.jacg.el.enums.ElConfigEnum.ECE_EXAMPLE) {
                continue;
            }
            OtherConfigItemVO config = new OtherConfigItemVO();
            config.setKey(configEnum.name());
            // 参数名称使用配置文件名，而不是枚举名称
            config.setName(configEnum.getKey());
            config.setFileName(configEnum.getKey());
            config.setDescription(Arrays.asList(configEnum.getDescriptions()));
            // 设置EL表达式允许使用的变量
            config.setElAllowedVariables(buildElAllowedVariables(configEnum.getElAllowedVariableEnums()));
            elConfig.add(config);
        }
        jacgDefs.setElConfig(elConfig);

        return jacgDefs;
    }

    /**
     * 构建EL表达式允许使用的变量列表
     *
     * @param allowedVariables 允许使用的变量枚举数组
     * @return 变量VO列表
     */
    private List<ElAllowedVariableVO> buildElAllowedVariables(ElAllowedVariableInterface[] allowedVariables) {
        if (allowedVariables == null || allowedVariables.length == 0) {
            return null;
        }
        List<ElAllowedVariableVO> result = new ArrayList<>();
        for (ElAllowedVariableInterface variable : allowedVariables) {
            ElAllowedVariableVO vo = new ElAllowedVariableVO();
            vo.setEnumConstantName(variable.getEnumConstantName());
            vo.setVariableName(variable.getVariableName());
            vo.setType(variable.getType());
            vo.setPrefixWithNum(variable.isPrefixWithNum());
            if (variable.getDescriptions() != null) {
                vo.setDescriptions(Arrays.asList(variable.getDescriptions()));
            }
            if (variable.getValueExamples() != null) {
                vo.setValueExamples(Arrays.asList(variable.getValueExamples()));
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 判断JACG主配置项是否可编辑
     */
    private boolean isJacgMainConfigEditable(ConfigKeyEnum configKey) {
        // OUTPUT_ROOT_PATH等禁止编辑
        return configKey != ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH
                && configKey != ConfigKeyEnum.CKE_OUTPUT_DIR_FLAG
                && configKey != ConfigKeyEnum.CKE_OUTPUT_DIR_NAME
                && configKey != ConfigKeyEnum.CKE_CALL_GRAPH_WRITE_TO_FILE
                && configKey != ConfigKeyEnum.CKE_CALL_GRAPH_RETURN_IN_MEMORY;
    }

    /**
     * 设置枚举选项（JACG）
     */
    private void setEnumOptions(ConfigItemVO config, ConfigKeyEnum configKey) {
        List<EnumOptionVO> enumOptions = null;

        // 根据配置项key判断是否为枚举类型
        switch (configKey) {
            case CKE_CALL_GRAPH_OUTPUT_DETAIL:
                enumOptions = buildOutputDetailEnumOptions();
                break;
            default:
                break;
        }

        config.setEnumOptions(enumOptions);
    }

    /**
     * 构建OutputDetailEnum选项
     */
    private List<EnumOptionVO> buildOutputDetailEnumOptions() {
        List<EnumOptionVO> options = new ArrayList<>();
        for (OutputDetailEnum enumValue : OutputDetailEnum.values()) {
            // 跳过非法值
            if (enumValue == OutputDetailEnum.ODE_ILLEGAL) {
                continue;
            }
            options.add(new EnumOptionVO(enumValue.getDetail(), enumValue.getDesc()));
        }
        return options;
    }

    /**
     * 设置数据库配置参数的依赖关系
     */
    private void setDbConfigDependency(ConfigItemVO config, ConfigDbKeyEnum configKey) {
        // 使用枚举字段获取名称，不硬编码
        String dbUseH2Key = ConfigDbKeyEnum.CDKE_DB_USE_H2.name();
        
        // 使用枚举字段直接比较，判断依赖关系
        // H2数据库文件路径：当db.use.h2=true时启用
        if (configKey == ConfigDbKeyEnum.CDKE_DB_H2_FILE_PATH) {
            config.setDependsOn(new DependsOnVO(dbUseH2Key, "true", "enable"));
        }
        // 非H2数据库配置（驱动、URL、用户名、密码）：当db.use.h2=false时启用
        else if (configKey == ConfigDbKeyEnum.CDKE_DB_DRIVER_NAME
                || configKey == ConfigDbKeyEnum.CDKE_DB_URL
                || configKey == ConfigDbKeyEnum.CDKE_DB_USERNAME
                || configKey == ConfigDbKeyEnum.CDKE_DB_PASSWORD) {
            config.setDependsOn(new DependsOnVO(dbUseH2Key, "false", "enable"));
        }
        // 其他配置参数（表后缀、慢查询相关）无依赖关系
    }

    /**
     * 设置Integer类型参数的范围限制
     */
    private void setIntegerRange(ConfigItemVO config) {
        // Integer类型参数设置最小值为0
        if ("Integer".equals(config.getType())) {
            config.setMinValue(0);
        }
    }

    /**
     * 判断是否为JavaCG2关键必填配置
     * 关键配置展示在外层界面，便于操作
     */
    private boolean isJavaCG2RequiredConfig(JavaCG2ConfigKeyEnum configKey) {
        // 使用ConfigVisibilityDefinition判断是否默认展示
        return ConfigVisibilityDefinition.isJavaCG2MainConfigVisible(configKey);
    }

    /**
     * 判断是否为JACG关键必填配置
     * 关键配置展示在外层界面，便于操作
     */
    private boolean isJacgRequiredConfig(ConfigKeyEnum configKey) {
        // 使用ConfigVisibilityDefinition判断是否默认展示
        return ConfigVisibilityDefinition.isJacgMainConfigVisible(configKey);
    }

    /**
     * 判断是否为数据库关键必填配置
     * 关键配置展示在外层界面，便于操作
     */
    private boolean isDbRequiredConfig(ConfigDbKeyEnum configKey) {
        // 使用ConfigVisibilityDefinition判断是否默认展示
        return ConfigVisibilityDefinition.isJacgDbConfigVisible(configKey);
    }

    @Override
    public String getElExampleContent(String type, String enumName) {
        if (type == null || enumName == null) {
            return null;
        }

        String configFileName;
        String examplePath;

        if ("javacg2".equalsIgnoreCase(type)) {
            // JavaCG2 EL配置
            try {
                com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum elConfigEnum = 
                        com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum.valueOf(enumName);
                configFileName = elConfigEnum.getKey();
            } catch (IllegalArgumentException e) {
                logger.warn("未找到JavaCG2 EL配置枚举: {}", enumName);
                return null;
            }
            // 示例文件路径: _el_example/{configFileName}.md
            examplePath = "_el_example/" + configFileName + ".md";
        } else if ("jacg".equalsIgnoreCase(type)) {
            // JACG EL配置
            try {
                com.adrninistrator.jacg.el.enums.ElConfigEnum elConfigEnum = 
                        com.adrninistrator.jacg.el.enums.ElConfigEnum.valueOf(enumName);
                configFileName = elConfigEnum.getKey();
            } catch (IllegalArgumentException e) {
                logger.warn("未找到JACG EL配置枚举: {}", enumName);
                return null;
            }
            // 示例文件路径: _el_example/{configFileName}.md
            examplePath = "_el_example/" + configFileName + ".md";
        } else {
            logger.warn("不支持的EL配置类型: {}", type);
            return null;
        }

        // 从classpath读取示例文件
        return readResourceFile(examplePath);
    }

    /**
     * 从classpath读取资源文件内容
     */
    private String readResourceFile(String path) {
        try {
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is == null) {
                logger.warn("未找到资源文件: {}", path);
                return null;
            }
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            logger.error("读取资源文件失败: {}", path, e);
            return null;
        }
    }

    /**
     * 从指定jar包中读取资源文件内容
     * 当classpath中多个jar包含同路径资源时，通过jar文件名关键字区分
     *
     * @param path           资源文件路径
     * @param jarNameKeyword jar文件名关键字，用于匹配目标jar包
     * @return 资源文件内容，未找到返回null
     */
    private String readResourceFileFromJar(String path, String jarNameKeyword) {
        try {
            java.util.Enumeration<java.net.URL> resources = getClass().getClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                java.net.URL url = resources.nextElement();
                String urlStr = url.toString();
                // 通过jar文件名关键字匹配目标jar包
                if (urlStr.contains(jarNameKeyword)) {
                    try (java.io.InputStream is = url.openStream();
                         java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, "UTF-8"))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        return sb.toString();
                    }
                }
            }
            logger.warn("未在包含关键字[{}]的jar包中找到资源文件: {}", jarNameKeyword, path);
            return null;
        } catch (Exception e) {
            logger.error("从jar包读取资源文件失败: {}, jar关键字: {}", path, jarNameKeyword, e);
            return null;
        }
    }

    @Override
    public List<ElMenuItemVO> getElVariableMenu(String type, String enumName) {
        List<ElMenuItemVO> variables = new ArrayList<>();
        
        if (type == null || enumName == null) {
            return variables;
        }

        ElAllowedVariableInterface[] allowedVariables = null;

        if ("javacg2".equalsIgnoreCase(type)) {
            // JavaCG2 EL配置
            try {
                com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum elConfigEnum = 
                        com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum.valueOf(enumName);
                allowedVariables = elConfigEnum.getElAllowedVariableEnums();
            } catch (IllegalArgumentException e) {
                logger.warn("未找到JavaCG2 EL配置枚举: {}", enumName);
                return variables;
            }
        } else if ("jacg".equalsIgnoreCase(type)) {
            // JACG EL配置
            try {
                com.adrninistrator.jacg.el.enums.ElConfigEnum elConfigEnum = 
                        com.adrninistrator.jacg.el.enums.ElConfigEnum.valueOf(enumName);
                allowedVariables = elConfigEnum.getElAllowedVariableEnums();
            } catch (IllegalArgumentException e) {
                logger.warn("未找到JACG EL配置枚举: {}", enumName);
                return variables;
            }
        } else {
            logger.warn("不支持的EL配置类型: {}", type);
            return variables;
        }

        // 遍历允许的变量，构建菜单项
        if (allowedVariables != null) {
            for (ElAllowedVariableInterface variable : allowedVariables) {
                String varName = variable.getVariableName();
                String[] descriptions = variable.getDescriptions();
                String displayText = varName + " - " + (descriptions != null && descriptions.length > 0 ? descriptions[0] : "");
                variables.add(new ElMenuItemVO(varName, displayText, varName));
            }
        }

        return variables;
    }

    @Override
    public List<ElMenuCategoryVO> getElFixedMenu() {
        List<ElMenuCategoryVO> categories = new ArrayList<>();

        // 逻辑运算符
        categories.add(new ElMenuCategoryVO("逻辑运算符", Arrays.asList(
                new ElMenuItemVO("&&", "&& - 逻辑与", "&&"),
                new ElMenuItemVO("||", "|| - 逻辑或", "||"),
                new ElMenuItemVO("!", "! - 逻辑非", "!"),
                new ElMenuItemVO("()", "() - 分组运算", "()")
        )));

        // 比较运算符
        categories.add(new ElMenuCategoryVO("比较运算符", Arrays.asList(
                new ElMenuItemVO("==", "== - 等于", "=="),
                new ElMenuItemVO("!=", "!= - 不等于", "!="),
                new ElMenuItemVO("<", "< - 小于", "<"),
                new ElMenuItemVO(">", "> - 大于", ">"),
                new ElMenuItemVO("<=", "<= - 小于等于", "<="),
                new ElMenuItemVO(">=", ">= - 大于等于", ">=")
        )));

        // 集合判断
        categories.add(new ElMenuCategoryVO("集合判断", Arrays.asList(
                new ElMenuItemVO("include", "include - 判断集合是否包含指定元素", "include(, )"),
                new ElMenuItemVO("!include", "!include - 判断集合是否不包含指定元素", "!include(, )")
        )));

        // 基本字符串方法
        categories.add(new ElMenuCategoryVO("基本字符串方法", Arrays.asList(
                new ElMenuItemVO("+", "+ - 字符串拼接", "+"),
                new ElMenuItemVO("string.startsWith()", "startsWith - 判断字符串是否以指定值开头", "string.startsWith(, )"),
                new ElMenuItemVO("string.endsWith()", "endsWith - 判断字符串是否以指定值结尾", "string.endsWith(, )"),
                new ElMenuItemVO("string.contains()", "contains - 判断字符串是否包含指定值", "string.contains(, )"),
                new ElMenuItemVO("string.length()", "length - 获取字符串长度", "string.length()")
        )));

        // 扩展字符串方法
        categories.add(new ElMenuCategoryVO("扩展字符串方法", Arrays.asList(
                new ElMenuItemVO("string.containsIC()", "containsIC - 判断字符串是否包含指定值(忽略大小写)", "string.containsIC(, )"),
                new ElMenuItemVO("string.endsWithIC()", "endsWithIC - 判断字符串是否以指定值结尾(忽略大小写)", "string.endsWithIC(, )"),
                new ElMenuItemVO("string.equalsIC()", "equalsIC - 判断字符串是否等于指定值(忽略大小写)", "string.equalsIC(, )"),
                new ElMenuItemVO("string.startsWithIC()", "startsWithIC - 判断字符串是否以指定值开头(忽略大小写)", "string.startsWithIC(, )"),
                new ElMenuItemVO("string.containsAny()", "containsAny - 判断字符串是否包含任意指定值", "string.containsAny(, , )"),
                new ElMenuItemVO("string.endsWithAny()", "endsWithAny - 判断字符串是否以任意指定值结尾", "string.endsWithAny(, , )"),
                new ElMenuItemVO("string.equalsAny()", "equalsAny - 判断字符串是否等于任意指定值", "string.equalsAny(, , )"),
                new ElMenuItemVO("string.startsWithAny()", "startsWithAny - 判断字符串是否以任意指定值开头", "string.startsWithAny(, , )")
        )));

        // 其他常量
        categories.add(new ElMenuCategoryVO("其他常量", Arrays.asList(
                new ElMenuItemVO("nil", "nil - 空值", "nil"),
                new ElMenuItemVO("true", "true - 真", "true"),
                new ElMenuItemVO("false", "false - 假", "false")
        )));

        return categories;
    }

    @Override
    public List<String> getConfigDescription(String configType, String enumName) {
        if (configType == null || enumName == null) {
            return null;
        }

        String[] descriptions = null;

        try {
            switch (configType.toLowerCase()) {
                case "javacg2-list":
                    JavaCG2OtherConfigFileUseListEnum listEnum = JavaCG2OtherConfigFileUseListEnum.valueOf(enumName);
                    descriptions = listEnum.getDescriptions();
                    break;
                case "javacg2-set":
                    JavaCG2OtherConfigFileUseSetEnum setEnum = JavaCG2OtherConfigFileUseSetEnum.valueOf(enumName);
                    descriptions = setEnum.getDescriptions();
                    break;
                case "jacg-list":
                    OtherConfigFileUseListEnum jacgListEnum = OtherConfigFileUseListEnum.valueOf(enumName);
                    descriptions = jacgListEnum.getDescriptions();
                    break;
                case "jacg-set":
                    OtherConfigFileUseSetEnum jacgSetEnum = OtherConfigFileUseSetEnum.valueOf(enumName);
                    descriptions = jacgSetEnum.getDescriptions();
                    break;
                default:
                    logger.warn("不支持的配置类型: {}", configType);
                    return null;
            }
        } catch (IllegalArgumentException e) {
            logger.warn("未找到配置枚举: {} - {}", configType, enumName);
            return null;
        }

        return descriptions != null ? Arrays.asList(descriptions) : null;
    }

    @Override
    public Map<String, Object> getConfigDetail(String configType, String enumName) {
        if (configType == null || enumName == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        String[] descriptions = null;
        String paramName = null;
        String fileName = null;

        try {
            switch (configType.toLowerCase()) {
                case "javacg2-list":
                    JavaCG2OtherConfigFileUseListEnum listEnum = JavaCG2OtherConfigFileUseListEnum.valueOf(enumName);
                    descriptions = listEnum.getDescriptions();
                    paramName = listEnum.getKey();
                    fileName = listEnum.getKey();
                    break;
                case "javacg2-set":
                    JavaCG2OtherConfigFileUseSetEnum setEnum = JavaCG2OtherConfigFileUseSetEnum.valueOf(enumName);
                    descriptions = setEnum.getDescriptions();
                    paramName = setEnum.getKey();
                    fileName = setEnum.getKey();
                    break;
                case "jacg-list":
                    OtherConfigFileUseListEnum jacgListEnum = OtherConfigFileUseListEnum.valueOf(enumName);
                    descriptions = jacgListEnum.getDescriptions();
                    paramName = jacgListEnum.getKey();
                    fileName = jacgListEnum.getKey();
                    break;
                case "jacg-set":
                    OtherConfigFileUseSetEnum jacgSetEnum = OtherConfigFileUseSetEnum.valueOf(enumName);
                    descriptions = jacgSetEnum.getDescriptions();
                    paramName = jacgSetEnum.getKey();
                    fileName = jacgSetEnum.getKey();
                    break;
                default:
                    logger.warn("不支持的配置类型: {}", configType);
                    return null;
            }
        } catch (IllegalArgumentException e) {
            logger.warn("未找到配置枚举: {} - {}", configType, enumName);
            return null;
        }

        if (descriptions != null) {
            result.put("paramName", paramName);
            result.put("fileName", fileName);
            result.put("descriptions", Arrays.asList(descriptions));
        }

        return result;
    }

    @Override
    public Map<String, Object> getEntryPointConfigDetail(String direction) {
        // 根据方向选择对应的枚举
        // caller=向下调用链，callee=向上调用链
        OtherConfigFileUseSetEnum setEnum;
        if ("callee".equals(direction)) {
            // 向上调用链
            setEnum = OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLEE;
 } else {
            // 向下调用链（默认）
            setEnum = OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLER;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("paramName", setEnum.getKey());
        result.put("fileName", setEnum.getKey());
        result.put("descriptions", Arrays.asList(setEnum.getDescriptions()));
        return result;
    }

    @Override
    public Map<String, Object> getFindStackKeywordConfigDetail(String direction) {
        // 根据方向选择对应的枚举
        // caller=向下调用链=查找被调用者=4ER，callee=向上调用链=查找调用者=4EE
        OtherConfigFileUseListEnum listEnum;
        if ("caller".equals(direction)) {
            // 向下调用链：查找被调用者
            listEnum = OtherConfigFileUseListEnum.OCFULE_FIND_STACK_KEYWORD_4ER;
        } else {
            // 向上调用链（默认）：查找调用者
            listEnum = OtherConfigFileUseListEnum.OCFULE_FIND_STACK_KEYWORD_4EE;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("paramName", listEnum.getKey());
        result.put("fileName", listEnum.getKey());
        result.put("descriptions", Arrays.asList(listEnum.getDescriptions()));
        return result;
    }

    @Override
    public String getElUsageContent(String type) {
        if (type == null) {
            return null;
        }

        ComponentEnum component = ComponentEnum.fromShortName(type);
        if (component == null) {
            logger.warn("不支持的EL配置类型: {}", type);
            return null;
        }

        String usagePath = "_el_example/el_usage.md";
        String jarNameKeyword = component.getFullName();

        // 由于classpath中两个jar包都包含同名资源文件 _el_example/el_usage.md，任意使用一个就可以
        // 不缓存文件内容
        return readResourceFileFromJar(usagePath, jarNameKeyword);
    }

    @Override
    public String findTemplateDir(String templateId, String projectId) {
        if (templateId == null || templateId.trim().isEmpty()) {
            return null;
        }

        String projectConfDir = getProjectConfDir();

        // 若指定了projectId，直接在对应项目目录下查找模板
        if (projectId != null && !projectId.trim().isEmpty()) {
            File templateDir = new File(new File(new File(projectConfDir, projectId), Constants.TEMPLATES_DIR), templateId);
            if (templateDir.exists() && templateDir.isDirectory()) {
                return templateDir.getAbsolutePath();
            }
            // 指定了projectId但未找到，不再遍历其他项目
            return null;
        }

        // 未指定projectId，遍历所有项目目录查找模板
        File projectConfDirFile = new File(projectConfDir);
        if (!projectConfDirFile.exists() || !projectConfDirFile.isDirectory()) {
            return null;
        }

        File[] projectDirs = projectConfDirFile.listFiles(File::isDirectory);
        if (projectDirs == null) {
            return null;
        }

        for (File projectDir : projectDirs) {
            File templatesDir = new File(projectDir, Constants.TEMPLATES_DIR);
            if (!templatesDir.exists() || !templatesDir.isDirectory()) {
                continue;
            }
            File templateDir = new File(templatesDir, templateId);
            if (templateDir.exists() && templateDir.isDirectory()) {
                return templateDir.getAbsolutePath();
            }
        }

        return null;
    }

    @Override
    public String getElUsageComponentContent(String type) {
        if (type == null) {
            return null;
        }

        ComponentEnum component = ComponentEnum.fromShortName(type);
        if (component == null) {
            logger.warn("不支持的EL配置类型: {}", type);
            return null;
        }

        // 组件通用说明文件路径: _el_example/el_usage_javacg2.md 或 _el_example/el_usage_jacg.md
        String usagePath = "_el_example/el_usage_" + component.getShortName() + ".md";
        String jarNameKeyword = component.getFullName();

        // 不缓存文件内容
        return readResourceFileFromJar(usagePath, jarNameKeyword);
    }
}
