package com.github.adrninistrator.jacgserver.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行任务日志管理器
 * 
 * 通过 Log4j2 编程式 API 为每个执行任务动态创建独立的 FileAppender，
 * 实现动态日志文件功能，支持并发执行。
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ExecutionLoggerManager {

    private static final String LOG_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} {%t} %-5p %c{1}.%M - %m%n";

    /**
     * 需要重定向日志的库包名
     */
    private static final String[] LIB_PACKAGES = {
            "com.adrninistrator.javacg2",
            "com.adrninistrator.jacg"
    };

    /**
     * 执行任务对应的Appender名称映射（用于追踪）
     */
    private static final ConcurrentHashMap<String, String> execAppenderMap = new ConcurrentHashMap<>();

    /**
     * 为指定执行任务创建日志Appender
     *
     * @param logId       日志ID（格式：projectId_logFileName）
     * @param logFilePath 日志文件完整路径
     */
    public static void createLogger(String logId, String logFilePath) {
        if (logId == null || logId.isEmpty()) {
            return;
        }

        // 确保日志目录存在
        File logFile = new File(logFilePath);
        File parentDir = logFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 获取LoggerContext
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration config = loggerContext.getConfiguration();

        // 创建布局
        Layout<String> layout = PatternLayout.newBuilder()
                .withPattern(LOG_PATTERN)
                .build();

        // 创建唯一的Appender名称
        String appenderName = "exec_" + logId;
        execAppenderMap.put(logId, appenderName);

        // 创建文件Appender
        FileAppender fileAppender = FileAppender.newBuilder()
                .setName(appenderName)
                .withFileName(logFilePath)
                .setLayout(layout)
                .setConfiguration(config)
                .build();

        // 启动Appender
        fileAppender.start();

        // 将Appender添加到配置
        config.addAppender(fileAppender);

        // 为库的Logger添加此Appender
        for (String packageName : LIB_PACKAGES) {
            LoggerConfig loggerConfig = config.getLoggerConfig(packageName);

            // 如果获取到的是Root Logger，需要创建新的LoggerConfig
            if (loggerConfig.getName().equals(LoggerConfig.ROOT)) {
                loggerConfig = LoggerConfig.newBuilder()
                        .withAdditivity(false)
                        .withLevel(Level.INFO)
                        .withLoggerName(packageName)
                        .withConfig(config)
                        .build();
                config.addLogger(packageName, loggerConfig);
            }

            // 添加Appender引用
            loggerConfig.addAppender(fileAppender, null, null);
        }

        // 更新LoggerContext
        loggerContext.updateLoggers();
    }

    /**
     * 移除指定执行任务的日志Appender
     *
     * @param logId 日志ID
     */
    public static void removeLogger(String logId) {
        if (logId == null || logId.isEmpty()) {
            return;
        }

        String appenderName = execAppenderMap.remove(logId);
        if (appenderName == null) {
            return;
        }

        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration config = loggerContext.getConfiguration();

        // 从库的Logger中移除Appender引用
        for (String packageName : LIB_PACKAGES) {
            LoggerConfig loggerConfig = config.getLoggerConfig(packageName);
            if (loggerConfig != null && !loggerConfig.getName().equals(LoggerConfig.ROOT)) {
                loggerConfig.removeAppender(appenderName);
            }
        }

        // 停止Appender（从LoggerConfig移除引用后，Appender不再被使用）
        Appender appender = config.getAppenders().get(appenderName);
        if (appender != null) {
            appender.stop();
        }

        // 更新LoggerContext
        loggerContext.updateLoggers();
    }

    /**
     * 获取执行任务的Appender名称
     *
     * @param logId 日志ID
     * @return Appender名称，如果不存在返回null
     */
    public static String getAppenderName(String logId) {
        return execAppenderMap.get(logId);
    }

    /**
     * 清理所有执行任务的日志配置
     */
    public static void cleanupAll() {
        for (Map.Entry<String, String> entry : execAppenderMap.entrySet()) {
            try {
                removeLogger(entry.getKey());
            } catch (Exception e) {
                // 忽略清理错误
            }
        }
        execAppenderMap.clear();
    }
}
