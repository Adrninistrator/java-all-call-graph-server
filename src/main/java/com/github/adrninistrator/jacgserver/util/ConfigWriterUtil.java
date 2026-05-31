package com.github.adrninistrator.jacgserver.util;

import com.adrninistrator.jacg.conf.ConfChecker;
import com.adrninistrator.jacg.conf.ConfigureWrapper;
import com.adrninistrator.jacg.conf.enums.ConfigDbKeyEnum;
import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseListEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseSetEnum;
import com.adrninistrator.jacg.conf.writer.JACGConfigWriter;
import com.adrninistrator.jacg.dboper.DbInitializer;
import com.adrninistrator.jacg.dboper.DbOperWrapper;
import com.adrninistrator.jacg.el.constants.ElConstants;
import com.adrninistrator.jacg.el.enums.ElConfigEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfManager;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseSetEnum;
import com.adrninistrator.javacg2.conf.writer.JavaCG2ConfigWriter;
import com.adrninistrator.javacg2.el.constants.JavaCG2ElConstants;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import com.github.adrninistrator.jacgserver.constant.ComponentEnum;
import com.github.adrninistrator.jacgserver.exception.ConfigConvertException;
import com.github.adrninistrator.jacgserver.exception.ConfigWriteException;
import com.github.adrninistrator.jacgserver.model.dto.JACGConfigDTO;
import com.github.adrninistrator.jacgserver.model.dto.JavaCG2ConfigDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 配置文件写入工具类
 * 使用JavaCG2ConfigWriter/JACGConfigWriter写入配置文件
 * 
 * 通过setBaseConfigureWrapper方法设置配置包装器，使配置文件中包含用户设置的值
 * 
 * 重要：
 * 1. 参数转换过程中如果出现异常会抛出ConfigConvertException，调用方需捕获处理
 * 2. 配置文件写入失败会抛出ConfigWriteException，调用方需捕获处理
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ConfigWriterUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConfigWriterUtil.class);

    private ConfigWriterUtil() {
    }

    /**
     * 写入JavaCG2配置文件
     *
     * @param projectDir 项目目录
     * @param config     JavaCG2配置DTO
     * @throws ConfigConvertException 参数转换异常
     * @throws ConfigWriteException 配置文件写入失败
     */
    public static void writeJavaCG2Config(String projectDir, JavaCG2ConfigDTO config) {
        if (config == null) {
            return;
        }
        doWriteJavaCG2Config(projectDir, config);
    }

    /**
     * 实际写入JavaCG2配置文件的方法
     * 
     * @throws ConfigWriteException 配置文件写入失败
     */
    private static void doWriteJavaCG2Config(String projectDir, JavaCG2ConfigDTO config) {
        // 创建JavaCG2ConfigureWrapper并设置配置值（转换异常会抛出）
        JavaCG2ConfigureWrapper wrapper = createJavaCG2ConfigureWrapper(config);

        // 调用JavaCG2ConfManager.getConfInfo()校验配置参数，若出现JavaCG2ConfigException则返回修改项目配置参数失败
        try {
            JavaCG2ConfManager.getConfInfo(wrapper);
        } catch (JavaCG2ConfigException e) {
            throw new ConfigConvertException("修改项目配置参数失败: " + e.getMessage(), e);
        }
        
        // 检查表达式配置参数（检查不通过会抛出异常）
        ElConfigValidator.validateJavaCG2ElConfig(wrapper);
        
        // 使用JavaCG2ConfigWriter写入配置文件
        JavaCG2ConfigWriter configWriter = new JavaCG2ConfigWriter(projectDir);
        configWriter.setBaseConfigureWrapper(wrapper);
        
        // 写入配置文件并检查返回值
        if (!configWriter.genMainConfig(JavaCG2ConfigKeyEnum.values())) {
            throw new ConfigWriteException(ComponentEnum.JAVACG2.getShortName() + "主配置文件写入失败");
        }
        if (!configWriter.genOtherConfig(JavaCG2OtherConfigFileUseListEnum.values())) {
            throw new ConfigWriteException(ComponentEnum.JAVACG2.getShortName() + "列表配置文件写入失败");
        }
        if (!configWriter.genOtherConfig(JavaCG2OtherConfigFileUseSetEnum.values())) {
            throw new ConfigWriteException(ComponentEnum.JAVACG2.getShortName() + " Set配置文件写入失败");
        }
        if (!configWriter.genElConfig(JavaCG2ElConfigEnum.values(), JavaCG2ElConstants.getElDirUsageMap())) {
            throw new ConfigWriteException(ComponentEnum.JAVACG2.getShortName() + " EL配置文件写入失败");
        }

        logger.info("{}配置文件写入完成: {}", ComponentEnum.JAVACG2.getShortName(), projectDir);
    }

    /**
     * 创建JavaCG2ConfigureWrapper并设置配置值
     * 
     * @param config JavaCG2配置DTO
     * @return JavaCG2ConfigureWrapper
     * @throws ConfigConvertException 参数转换异常
     */
    private static JavaCG2ConfigureWrapper createJavaCG2ConfigureWrapper(JavaCG2ConfigDTO config) {
        JavaCG2ConfigureWrapper wrapper = new JavaCG2ConfigureWrapper();
        
        // 设置主配置值
        Map<String, ?> mainConfig = config.getMainConfig();
        if (mainConfig != null) {
            for (Map.Entry<String, ?> entry : mainConfig.entrySet()) {
                JavaCG2ConfigKeyEnum configKeyEnum;
                try {
                    configKeyEnum = JavaCG2ConfigKeyEnum.valueOf(entry.getKey());
                } catch (IllegalArgumentException e) {
                    throw new ConfigConvertException("未知的" + ComponentEnum.JAVACG2.getShortName() + "主配置项: " + entry.getKey(), e);
                }
                // 将值转换为字符串（处理Boolean等非String类型）
                String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : null;
                wrapper.setMainConfig(configKeyEnum, value);
            }
        }
        
        // 设置List配置值
        Map<String, List<String>> listConfig = config.getListConfig();
        if (listConfig != null) {
            for (Map.Entry<String, List<String>> entry : listConfig.entrySet()) {
                JavaCG2OtherConfigFileUseListEnum configEnum;
                try {
                    configEnum = JavaCG2OtherConfigFileUseListEnum.valueOf(entry.getKey());
                } catch (IllegalArgumentException e) {
                    throw new ConfigConvertException("未知的" + ComponentEnum.JAVACG2.getShortName() + " List配置项: " + entry.getKey(), e);
                }
                wrapper.setOtherConfigList(configEnum, entry.getValue());
            }
        }
        
        // 设置Set配置值
        Map<String, List<String>> setConfig = config.getSetConfig();
        if (setConfig != null) {
            for (Map.Entry<String, List<String>> entry : setConfig.entrySet()) {
                JavaCG2OtherConfigFileUseSetEnum configEnum;
                try {
                    configEnum = JavaCG2OtherConfigFileUseSetEnum.valueOf(entry.getKey());
                } catch (IllegalArgumentException e) {
                    throw new ConfigConvertException("未知的" + ComponentEnum.JAVACG2.getShortName() + " Set配置项: " + entry.getKey(), e);
                }
                // 转换为Set
                Set<String> valueSet = new HashSet<>(entry.getValue());
                wrapper.setOtherConfigSet(configEnum, valueSet);
            }
        }
        
        // 设置EL配置值
        Map<String, ?> elConfig = config.getElConfig();
        if (elConfig != null) {
            for (Map.Entry<String, ?> entry : elConfig.entrySet()) {
                JavaCG2ElConfigEnum configEnum;
                try {
                    configEnum = JavaCG2ElConfigEnum.valueOf(entry.getKey());
                } catch (IllegalArgumentException e) {
                    throw new ConfigConvertException("未知的" + ComponentEnum.JAVACG2.getShortName() + " EL配置项: " + entry.getKey(), e);
                }
                String value = convertElConfigValue(entry.getValue());
                wrapper.setElConfigText(configEnum, value);
            }
        }
        
        return wrapper;
    }

    /**
     * 写入JACG配置文件
     *
     * @param configDir 配置目录（项目目录或模板目录）
     * @param config    JACG配置DTO
     * @throws ConfigConvertException 参数转换异常
     * @throws ConfigWriteException 配置文件写入失败
     */
    public static void writeJACGConfig(String configDir, JACGConfigDTO config) {
        if (config == null) {
            config = new JACGConfigDTO();
        }
        doWriteJACGConfig(configDir, config);
    }

    /**
     * 实际写入JACG配置文件的方法
     *
     * @throws ConfigWriteException 配置文件写入失败
     */
    private static void doWriteJACGConfig(String configDir, JACGConfigDTO config) {
        // 创建ConfigureWrapper并设置配置值（转换异常会抛出）
        ConfigureWrapper wrapper = createConfigureWrapper(configDir, config);

        // 调用DbInitializer.genDbOperWrapper验证数据库配置是否正确，参数2固定使用true
        // 仅在以下情况需要验证：
        // 1. 使用非H2数据库
        // 2. 使用H2数据库，且对应数据库文件已存在（在H2数据库文件路径后加上".mv.db"判断是否存在）
        if (needValidateDbConfig(wrapper)) {
            try {
                DbOperWrapper dbOperWrapper = DbInitializer.genDbOperWrapper(wrapper, false, true, ConfigWriterUtil.class);
                if (dbOperWrapper != null) {
                    dbOperWrapper.getDbOperator().close();
                }
            } catch (Exception e) {
                throw new ConfigConvertException("修改项目配置参数失败，数据库配置不正确: " + e.getMessage(), e);
            }
        }

        // 调用ConfChecker.checkAll检查配置参数，若出现异常则返回修改项目配置参数失败
        try {
            ConfChecker.checkAll(wrapper);
        } catch (Exception e) {
            throw new ConfigConvertException("修改项目配置参数失败: " + e.getMessage(), e);
        }

        // 检查表达式配置参数（检查不通过会抛出异常）
        ElConfigValidator.validateJACGElConfig(wrapper);

        // 使用JACGConfigWriter写入配置文件
        JACGConfigWriter configWriter = new JACGConfigWriter(configDir);
        configWriter.setBaseConfigureWrapper(wrapper);

        // 写入配置文件并检查返回值
        if (!configWriter.genMainConfig(ConfigKeyEnum.values())) {
            throw new ConfigWriteException(ComponentEnum.JACG.getShortName() + "主配置文件写入失败");
        }
        if (!configWriter.genMainConfig(ConfigDbKeyEnum.values())) {
            throw new ConfigWriteException(ComponentEnum.JACG.getShortName() + "数据库配置文件写入失败");
        }
        if (!configWriter.genOtherConfig(OtherConfigFileUseListEnum.values())) {
            throw new ConfigWriteException(ComponentEnum.JACG.getShortName() + "列表配置文件写入失败");
        }
        if (!configWriter.genOtherConfig(OtherConfigFileUseSetEnum.values())) {
            throw new ConfigWriteException(ComponentEnum.JACG.getShortName() + " Set配置文件写入失败");
        }
        if (!configWriter.genElConfig(ElConfigEnum.values(), ElConstants.getElDirUsageMap())) {
            throw new ConfigWriteException(ComponentEnum.JACG.getShortName() + " EL配置文件写入失败");
        }

        logger.info("{}配置文件写入完成: {}", ComponentEnum.JACG.getShortName(), configDir);
    }

    /**
     * 判断是否需要验证数据库配置
     * 仅在以下情况需要验证：
     * 1. 使用非H2数据库
     * 2. 使用H2数据库，且对应数据库文件已存在（在H2数据库文件路径后加上".mv.db"判断是否存在）
     *
     * @param wrapper ConfigureWrapper对象
     * @return 是否需要验证
     */
    private static boolean needValidateDbConfig(ConfigureWrapper wrapper) {
        boolean useH2 = wrapper.getMainConfig(ConfigDbKeyEnum.CDKE_DB_USE_H2);
        if (!useH2) {
            // 使用非H2数据库，需要验证
            logger.info("使用非H2数据库，需要验证数据库配置");
            return true;
        }

        // 使用H2数据库，判断数据库文件是否已存在
        String h2FilePath = wrapper.getMainConfig(ConfigDbKeyEnum.CDKE_DB_H2_FILE_PATH);
        if (h2FilePath == null || h2FilePath.isEmpty()) {
            logger.info("使用H2数据库，H2文件路径为空，不需要验证数据库配置");
            return false;
        }

        // 在H2数据库文件路径后加上".mv.db"判断是否存在
        String h2MvDbFilePath = h2FilePath + ".mv.db";
        File h2MvDbFile = new File(h2MvDbFilePath);
        if (h2MvDbFile.exists()) {
            logger.info("使用H2数据库，数据库文件已存在: {}，需要验证数据库配置", h2MvDbFilePath);
            return true;
        }

        logger.info("使用H2数据库，数据库文件不存在: {}，不需要验证数据库配置", h2MvDbFilePath);
        return false;
    }

    /**
     * 创建ConfigureWrapper并设置配置值（包含默认值处理）
     *
     * @param configDir 配置目录（用于设置默认的H2数据库路径）
     * @param config JACG配置DTO
     * @return ConfigureWrapper
     * @throws ConfigConvertException 参数转换异常
     */
    private static ConfigureWrapper createConfigureWrapper(String configDir, JACGConfigDTO config) {
        ConfigureWrapper wrapper = new ConfigureWrapper();
        
        // 设置主配置值（ConfigKeyEnum）
        Map<String, ?> mainConfig = config.getMainConfig();
        if (mainConfig != null) {
            for (Map.Entry<String, ?> entry : mainConfig.entrySet()) {
                ConfigKeyEnum configKeyEnum;
                try {
                    configKeyEnum = ConfigKeyEnum.valueOf(entry.getKey());
                } catch (IllegalArgumentException e) {
                    throw new ConfigConvertException("未知的" + ComponentEnum.JACG.getShortName() + "主配置项: " + entry.getKey(), e);
                }
                // 将值转换为字符串（处理Boolean等非String类型）
                String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : null;
                wrapper.setMainConfig(configKeyEnum, value);
            }
        }
        
        // 设置数据库配置值（ConfigDbKeyEnum）
        Map<String, ?> dbConfig = config.getDbConfig();
        if (dbConfig != null) {
            for (Map.Entry<String, ?> entry : dbConfig.entrySet()) {
                ConfigDbKeyEnum configDbKeyEnum;
                try {
                    configDbKeyEnum = ConfigDbKeyEnum.valueOf(entry.getKey());
                } catch (IllegalArgumentException e) {
                    throw new ConfigConvertException("未知的" + ComponentEnum.JACG.getShortName() + "数据库配置项: " + entry.getKey(), e);
                }
                // 将值转换为字符串（处理Boolean等非String类型）
                String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : null;
                wrapper.setMainConfig(configDbKeyEnum, value);
            }
        }

        // 设置数据库配置默认值
        // 默认使用H2数据库
        if (dbConfig == null || !dbConfig.containsKey(ConfigDbKeyEnum.CDKE_DB_USE_H2.name())) {
            wrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_USE_H2, "true");
        }
        // 默认H2数据库文件路径为配置目录下的h2db/jacg文件
        if (dbConfig == null || !dbConfig.containsKey(ConfigDbKeyEnum.CDKE_DB_H2_FILE_PATH.name())) {
            // 使用File类规范化路径，避免手动处理分隔符产生双斜杠
            File configDirFile = new File(configDir);
            File h2DbDir = new File(new File(configDirFile, "h2db"), "jacg");
            // 统一使用/作为分隔符，确保跨平台一致性
            String defaultH2DbPath = h2DbDir.getPath().replace('\\', '/');
            // 确保相对路径以./开头
            if (!defaultH2DbPath.startsWith("./") && !defaultH2DbPath.startsWith("/") && 
                !defaultH2DbPath.matches("^[A-Za-z]:.*")) {
                // 不是绝对路径（Windows的C:或Unix的/），且不以./开头，则添加./前缀
                defaultH2DbPath = "./" + defaultH2DbPath;
            }
            wrapper.setMainConfig(ConfigDbKeyEnum.CDKE_DB_H2_FILE_PATH, defaultH2DbPath);
        }
        
        // 设置List配置值
        Map<String, List<String>> listConfig = config.getListConfig();
        if (listConfig != null) {
            for (Map.Entry<String, List<String>> entry : listConfig.entrySet()) {
                OtherConfigFileUseListEnum configEnum;
                try {
                    configEnum = OtherConfigFileUseListEnum.valueOf(entry.getKey());
                } catch (IllegalArgumentException e) {
                    throw new ConfigConvertException("未知的" + ComponentEnum.JACG.getShortName() + " List配置项: " + entry.getKey(), e);
                }
                wrapper.setOtherConfigList(configEnum, entry.getValue());
            }
        }
        
        // 设置Set配置值
        Map<String, List<String>> setConfig = config.getSetConfig();
        if (setConfig != null) {
            for (Map.Entry<String, List<String>> entry : setConfig.entrySet()) {
                OtherConfigFileUseSetEnum configEnum;
                try {
                    configEnum = OtherConfigFileUseSetEnum.valueOf(entry.getKey());
                } catch (IllegalArgumentException e) {
                    throw new ConfigConvertException("未知的" + ComponentEnum.JACG.getShortName() + " Set配置项: " + entry.getKey(), e);
                }
                // 转换为Set
                Set<String> valueSet = new HashSet<>(entry.getValue());
                wrapper.setOtherConfigSet(configEnum, valueSet);
            }
        }
        
        // 设置EL配置值
        Map<String, ?> elConfig = config.getElConfig();
        if (elConfig != null) {
            for (Map.Entry<String, ?> entry : elConfig.entrySet()) {
                ElConfigEnum configEnum;
                try {
                    configEnum = ElConfigEnum.valueOf(entry.getKey());
                } catch (IllegalArgumentException e) {
                    throw new ConfigConvertException("未知的" + ComponentEnum.JACG.getShortName() + " EL配置项: " + entry.getKey(), e);
                }
                String value = convertElConfigValue(entry.getValue());
                wrapper.setElConfigText(configEnum, value);
            }
        }
        
        return wrapper;
    }

    /**
     * 将EL配置值转换为字符串
     * EL表达式配置为单个字符串，若前端传入List（如["false"]），需要提取第一个元素而非使用toString()
     * 避免List.toString()产生"[false]"格式导致EL解析失败
     *
     * @param value EL配置值（可能是String、Boolean、List等类型）
     * @return 字符串形式的EL配置值
     */
    private static String convertElConfigValue(Object value) {
        if (value == null) {
            return null;
        }
        // 若值为List类型，取第一个元素（EL配置为单个字符串，不应为数组）
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return null;
            }
            // 取第一个元素并转换为字符串
            Object first = list.get(0);
            return first != null ? first.toString() : null;
        }
        return value.toString();
    }
}
