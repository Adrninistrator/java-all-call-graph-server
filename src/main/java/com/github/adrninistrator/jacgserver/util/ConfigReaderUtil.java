package com.github.adrninistrator.jacgserver.util;

import com.adrninistrator.jacg.conf.ConfigureWrapper;
import com.adrninistrator.jacg.conf.enums.ConfigDbKeyEnum;
import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseListEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseSetEnum;
import com.adrninistrator.jacg.el.enums.ElConfigEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseSetEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.github.adrninistrator.jacgserver.model.dto.JACGConfigDTO;
import com.github.adrninistrator.jacgserver.model.dto.JavaCG2ConfigDTO;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 配置文件读取工具类
 * 使用ConfigureWrapper/JavaCG2ConfigureWrapper从配置文件读取参数值
 * 
 * 注意：获取配置参数方法时，useConfig参数设为false（仅读取配置参数，不执行使用时的检查）
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ConfigReaderUtil {

    /**
     * useConfig参数值：false表示仅读取配置参数，不执行使用时的检查
     */
    private static final boolean USE_CONFIG_FALSE = false;

    private ConfigReaderUtil() {
    }

    /**
     * 从项目目录读取JavaCG2配置
     *
     * @param projectDir 项目目录
     * @return JavaCG2配置DTO
     */
    public static JavaCG2ConfigDTO readJavaCG2Config(String projectDir) {
        // 设置默认MDC变量，确保库日志能正确输出
        MDCUtil.setDefaultMDC();
        try {
            return doReadJavaCG2Config(projectDir);
        } finally {
            MDCUtil.clearMDC();
        }
    }

    /**
     * 实际读取JavaCG2配置的方法
     */
    private static JavaCG2ConfigDTO doReadJavaCG2Config(String projectDir) {
        JavaCG2ConfigureWrapper wrapper = new JavaCG2ConfigureWrapper(false, projectDir);
        JavaCG2ConfigDTO dto = new JavaCG2ConfigDTO();

        // 读取主配置（useConfig=false，仅读取配置参数）
        Map<String, Object> mainConfig = new HashMap<>();
        for (JavaCG2ConfigKeyEnum configKey : JavaCG2ConfigKeyEnum.values()) {
            Object value = wrapper.getMainConfig(configKey, USE_CONFIG_FALSE);
            if (value != null) {
                mainConfig.put(configKey.name(), value.toString());
            }
        }
        dto.setMainConfig(mainConfig);

        // 读取List配置（useConfig=false，仅读取配置参数）
        Map<String, List<String>> listConfig = new HashMap<>();
        for (JavaCG2OtherConfigFileUseListEnum configEnum : JavaCG2OtherConfigFileUseListEnum.values()) {
            List<String> value = wrapper.getOtherConfigList(configEnum, USE_CONFIG_FALSE);
            if (value != null && !value.isEmpty()) {
                listConfig.put(configEnum.name(), value);
            }
        }
        dto.setListConfig(listConfig);

        // 读取Set配置（useConfig=false，仅读取配置参数）
        Map<String, List<String>> setConfig = new HashMap<>();
        for (JavaCG2OtherConfigFileUseSetEnum configEnum : JavaCG2OtherConfigFileUseSetEnum.values()) {
            Set<String> value = wrapper.getOtherConfigSet(configEnum, USE_CONFIG_FALSE);
            if (value != null && !value.isEmpty()) {
                setConfig.put(configEnum.name(), new ArrayList<>(value));
            }
        }
        dto.setSetConfig(setConfig);

        // 读取EL配置
        Map<String, Object> elConfig = new HashMap<>();
        for (JavaCG2ElConfigEnum configEnum : JavaCG2ElConfigEnum.values()) {
            String value = wrapper.getElConfigText(configEnum);
            if (value != null && !value.isEmpty()) {
                elConfig.put(configEnum.name(), value);
            }
        }
        dto.setElConfig(elConfig);

        return dto;
    }

    /**
     * 从项目或模板目录读取JACG配置
     *
     * @param configDir 配置目录（项目目录或模板目录）
     * @return JACG配置DTO
     */
    public static JACGConfigDTO readJACGConfig(String configDir) {
        // 设置默认MDC变量，确保库日志能正确输出
        MDCUtil.setDefaultMDC();
        try {
            return doReadJACGConfig(configDir);
        } finally {
            MDCUtil.clearMDC();
        }
    }

    /**
     * 实际读取JACG配置的方法
     */
    private static JACGConfigDTO doReadJACGConfig(String configDir) {
        // 检查配置目录是否存在
        if (!new File(configDir).exists()) {
            return new JACGConfigDTO();
        }

        ConfigureWrapper wrapper = new ConfigureWrapper(false, configDir);
        JACGConfigDTO dto = new JACGConfigDTO();

        // 读取主配置（ConfigKeyEnum）（useConfig=false，仅读取配置参数）
        Map<String, Object> mainConfig = new HashMap<>();
        for (ConfigKeyEnum configKey : ConfigKeyEnum.values()) {
            Object value = wrapper.getMainConfig(configKey, USE_CONFIG_FALSE);
            if (value != null) {
                mainConfig.put(configKey.name(), value.toString());
            }
        }
        dto.setMainConfig(mainConfig);

        // 读取数据库配置（ConfigDbKeyEnum）（useConfig=false，仅读取配置参数）
        Map<String, Object> dbConfig = new HashMap<>();
        for (ConfigDbKeyEnum configKey : ConfigDbKeyEnum.values()) {
            Object value = wrapper.getMainConfig(configKey, USE_CONFIG_FALSE);
            if (value != null) {
                dbConfig.put(configKey.name(), value.toString());
            }
        }
        dto.setDbConfig(dbConfig);

        // 读取List配置（useConfig=false，仅读取配置参数）
        Map<String, List<String>> listConfig = new HashMap<>();
        for (OtherConfigFileUseListEnum configEnum : OtherConfigFileUseListEnum.values()) {
            List<String> value = wrapper.getOtherConfigList(configEnum, USE_CONFIG_FALSE);
            if (value != null && !value.isEmpty()) {
                listConfig.put(configEnum.name(), value);
            }
        }
        dto.setListConfig(listConfig);

        // 读取Set配置（useConfig=false，仅读取配置参数）
        Map<String, List<String>> setConfig = new HashMap<>();
        for (OtherConfigFileUseSetEnum configEnum : OtherConfigFileUseSetEnum.values()) {
            Set<String> value = wrapper.getOtherConfigSet(configEnum, USE_CONFIG_FALSE);
            if (value != null && !value.isEmpty()) {
                setConfig.put(configEnum.name(), new ArrayList<>(value));
            }
        }
        dto.setSetConfig(setConfig);

        // 读取EL配置
        Map<String, Object> elConfig = new HashMap<>();
        for (ElConfigEnum configEnum : ElConfigEnum.values()) {
            String value = wrapper.getElConfigText(configEnum);
            if (value != null && !value.isEmpty()) {
                elConfig.put(configEnum.name(), value);
            }
        }
        dto.setElConfig(elConfig);

        return dto;
    }

    /**
     * 从项目目录读取JavaCG2 EL配置
     *
     * @param projectDir 项目目录
     * @return EL配置Map
     */
    public static Map<String, String> readJavaCG2ElConfig(String projectDir) {
        // 设置默认MDC变量，确保库日志能正确输出
        MDCUtil.setDefaultMDC();
        try {
            JavaCG2ConfigureWrapper wrapper = new JavaCG2ConfigureWrapper(false, projectDir);
            Map<String, String> elConfig = new HashMap<>();

            for (JavaCG2ElConfigEnum configEnum : JavaCG2ElConfigEnum.values()) {
                String value = wrapper.getElConfigText(configEnum);
                if (value != null && !value.isEmpty()) {
                    elConfig.put(configEnum.name(), value);
                }
            }
            return elConfig;
        } finally {
            MDCUtil.clearMDC();
        }
    }

    /**
     * 从项目或模板目录读取JACG EL配置
     *
     * @param configDir 配置目录
     * @return EL配置Map
     */
    public static Map<String, String> readJACGElConfig(String configDir) {
        // 设置默认MDC变量，确保库日志能正确输出
        MDCUtil.setDefaultMDC();
        try {
            if (!new File(configDir).exists()) {
                return new HashMap<>();
            }

            ConfigureWrapper wrapper = new ConfigureWrapper(false, configDir);
            Map<String, String> elConfig = new HashMap<>();

            for (ElConfigEnum configEnum : ElConfigEnum.values()) {
                String value = wrapper.getElConfigText(configEnum);
                if (value != null && !value.isEmpty()) {
                    elConfig.put(configEnum.name(), value);
                }
            }
            return elConfig;
        } finally {
            MDCUtil.clearMDC();
        }
    }
}
