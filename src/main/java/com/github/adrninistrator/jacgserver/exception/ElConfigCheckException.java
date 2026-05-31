package com.github.adrninistrator.jacgserver.exception;

import com.github.adrninistrator.jacgserver.constant.ErrorCode;

/**
 * 表达式配置参数检查异常
 * 
 * 当表达式配置参数检查不通过时抛出此异常
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ElConfigCheckException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 配置来源（javacg2 或 jacg）
     */
    private final String configSource;

    /**
     * 检查不通过的表达式枚举名称
     */
    private final String elConfigEnumName;

    /**
     * 配置文件名
     */
    private final String configFileName;

    /**
     * 配置描述
     */
    private final String configDescription;

    /**
     * 表达式字符串
     */
    private final String elText;

    /**
     * 错误信息（异常的getMessage返回值）
     */
    private final String errorMessage;

    /**
     * 构造函数
     *
     * @param configSource    配置来源（javacg2 或 jacg）
     * @param elConfigEnumName 检查不通过的表达式枚举名称
     * @param configFileName  配置文件名
     * @param configDescription 配置描述
     * @param elText          表达式字符串
     * @param errorMessage    错误信息（异常的getMessage返回值）
     */
    public ElConfigCheckException(String configSource, String elConfigEnumName, String configFileName, 
            String configDescription, String elText, String errorMessage) {
        super(ErrorCode.BAD_REQUEST, formatMessage(elConfigEnumName, configFileName, configDescription, elText, errorMessage));
        this.configSource = configSource;
        this.elConfigEnumName = elConfigEnumName;
        this.configFileName = configFileName;
        this.configDescription = configDescription;
        this.elText = elText;
        this.errorMessage = errorMessage;
    }

    /**
     * 格式化错误信息
     */
    private static String formatMessage(String elConfigEnumName, String configFileName, 
            String configDescription, String elText, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        if (elConfigEnumName != null && !elConfigEnumName.isEmpty()) {
            sb.append("表达式配置[").append(elConfigEnumName).append("]检查失败");
        } else {
            sb.append("表达式配置检查失败");
        }
        if (errorMessage != null && !errorMessage.isEmpty()) {
            sb.append("，错误信息: ").append(errorMessage);
        }
        return sb.toString();
    }

    /**
     * 获取配置来源
     *
     * @return 配置来源（javacg2 或 jacg）
     */
    public String getConfigSource() {
        return configSource;
    }

    /**
     * 获取检查不通过的表达式枚举名称
     *
     * @return 表达式枚举名称
     */
    public String getElConfigEnumName() {
        return elConfigEnumName;
    }

    /**
     * 获取配置文件名
     *
     * @return 配置文件名
     */
    public String getConfigFileName() {
        return configFileName;
    }

    /**
     * 获取配置描述
     *
     * @return 配置描述
     */
    public String getConfigDescription() {
        return configDescription;
    }

    /**
     * 获取表达式字符串
     *
     * @return 表达式字符串
     */
    public String getElText() {
        return elText;
    }

    /**
     * 获取错误信息
     *
     * @return 错误信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
