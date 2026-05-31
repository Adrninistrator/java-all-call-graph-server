package com.github.adrninistrator.jacgserver.util;

import com.adrninistrator.jacg.conf.ConfigureWrapper;
import com.adrninistrator.jacg.el.enums.ElConfigEnum;
import com.adrninistrator.jacg.el.manager.ElManager;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import com.adrninistrator.javacg2.el.enums.interfaces.ElConfigInterface;
import com.adrninistrator.javacg2.el.manager.JavaCG2ElManager;
import com.adrninistrator.javacg2.exceptions.JavaCG2ElConfigRuntimeException;
import com.github.adrninistrator.jacgserver.constant.ComponentEnum;
import com.github.adrninistrator.jacgserver.exception.ElConfigCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 表达式配置参数检查工具类
 * 
 * 在保存项目或模板时，需要对表达式配置参数进行检查
 * 如果检查不通过，会抛出ElConfigCheckException异常
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ElConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(ElConfigValidator.class);

    private ElConfigValidator() {
    }

    /**
     * 检查JavaCG2表达式配置参数
     *
     * @param wrapper JavaCG2配置包装器
     * @throws ElConfigCheckException 检查不通过时抛出
     */
    public static void validateJavaCG2ElConfig(JavaCG2ConfigureWrapper wrapper) {
        if (wrapper == null) {
            return;
        }

        try {
            // 创建JavaCG2ElManager进行检查
            JavaCG2ElManager javacg2ElManager = new JavaCG2ElManager(wrapper, JavaCG2ElConfigEnum.values(), null);
            logger.info("{}表达式配置参数检查通过", ComponentEnum.JAVACG2.getShortName());
        } catch (JavaCG2ElConfigRuntimeException e) {
            // 获取检查不通过的表达式枚举
            ElConfigInterface elConfig = e.getElConfig();
            String elConfigEnumName = getElConfigName(elConfig);
            // 获取配置文件名
            String configFileName = elConfig.getKey();
            // 获取配置描述（取描述数组的第一个元素）
            String configDescription = getElConfigDescription(elConfig);
            // 获取表达式字符串
            String elText = wrapper.getElConfigText(elConfig);
            String errorMessage = e.getMessage();
            
            logger.error("{}表达式配置参数检查失败: {} - {}", ComponentEnum.JAVACG2.getShortName(), elConfigEnumName, errorMessage);
            throw new ElConfigCheckException(ComponentEnum.JAVACG2.getShortName(), elConfigEnumName, configFileName, configDescription, elText, errorMessage);
        }
    }

    /**
     * 检查JACG表达式配置参数
     *
     * @param wrapper JACG配置包装器
     * @throws ElConfigCheckException 检查不通过时抛出
     */
    public static void validateJACGElConfig(ConfigureWrapper wrapper) {
        if (wrapper == null) {
            return;
        }

        try {
            // 创建ElManager进行检查
            ElManager elManager = new ElManager(wrapper, ElConfigEnum.values(), null);
            logger.info("{}表达式配置参数检查通过", ComponentEnum.JACG.getShortName());
        } catch (JavaCG2ElConfigRuntimeException e) {
            // 获取检查不通过的表达式枚举
            ElConfigInterface elConfig = e.getElConfig();
            String elConfigEnumName = getElConfigName(elConfig);
            // 获取配置文件名
            String configFileName = elConfig.getKey();
            // 获取配置描述（取描述数组的第一个元素）
            String configDescription = getElConfigDescription(elConfig);
            // 获取表达式字符串
            String elText = wrapper.getElConfigText(elConfig);
            String errorMessage = e.getMessage();
            
            logger.error("{}表达式配置参数检查失败: {} - {}", ComponentEnum.JACG.getShortName(), elConfigEnumName, errorMessage);
            throw new ElConfigCheckException(ComponentEnum.JACG.getShortName(), elConfigEnumName, configFileName, configDescription, elText, errorMessage);
        }
    }

    /**
     * 获取表达式配置枚举名称
     * ElConfigInterface由枚举实现，可以转换为Enum获取name
     *
     * @param elConfig 表达式配置接口
     * @return 枚举名称
     */
    private static String getElConfigName(ElConfigInterface elConfig) {
        if (elConfig == null) {
            return "";
        }
        // ElConfigInterface由枚举实现，可以转换为Enum获取name
        if (elConfig instanceof Enum) {
            return ((Enum<?>) elConfig).name();
        }
        return elConfig.toString();
    }

    /**
     * 获取表达式配置描述
     * 取描述数组的第一个元素
     *
     * @param elConfig 表达式配置接口
     * @return 配置描述
     */
    private static String getElConfigDescription(ElConfigInterface elConfig) {
        if (elConfig == null) {
            return "";
        }
        String[] descriptions = elConfig.getDescriptions();
        if (descriptions != null && descriptions.length > 0) {
            return descriptions[0];
        }
        return "";
    }
}
