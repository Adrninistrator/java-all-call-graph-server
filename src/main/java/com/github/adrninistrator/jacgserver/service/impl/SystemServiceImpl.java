package com.github.adrninistrator.jacgserver.service.impl;

import com.github.adrninistrator.jacgserver.model.entity.CallGraphExecutionRecord;
import com.github.adrninistrator.jacgserver.model.entity.FindStackExecutionRecord;
import com.github.adrninistrator.jacgserver.service.SystemService;
import com.github.adrninistrator.jacgserver.service.TemplateExecutionRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

/**
 * 系统服务实现类
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Service
public class SystemServiceImpl implements SystemService {

    private static final Logger logger = LoggerFactory.getLogger(SystemServiceImpl.class);

    @Resource
    private TemplateExecutionRecordService templateExecutionRecordService;

    @Override
    public OpenDirectoryResult openDirectory(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            logger.error("目录路径为空");
            return new OpenDirectoryResult(false, "目录路径不能为空");
        }

        try {
            // 规范化路径，去除.、..等相对路径符号
            File directory = new File(directoryPath).getCanonicalFile();
            String normalizedPath = directory.getAbsolutePath();

            if (!directory.exists()) {
                logger.error("目录不存在: {} (原始路径: {})", normalizedPath, directoryPath);
                return new OpenDirectoryResult(false, "目录不存在: " + normalizedPath);
            }

            if (!directory.isDirectory()) {
                logger.error("路径不是目录: {} (原始路径: {})", normalizedPath, directoryPath);
                return new OpenDirectoryResult(false, "路径不是目录: " + normalizedPath);
            }

            // 获取操作系统名称
            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("win")) {
                // Windows系统使用explorer命令
                ProcessBuilder pb = new ProcessBuilder("explorer", normalizedPath);
                pb.start();
                logger.info("已打开目录: {}", normalizedPath);
                return new OpenDirectoryResult(true, "目录已打开");
            } else {
                logger.warn("当前操作系统[{}]不支持打开目录功能，仅支持Windows系统", osName);
                return new OpenDirectoryResult(false, "不支持当前操作系统，仅支持Windows系统");
            }
        } catch (IOException e) {
            logger.error("打开目录失败: {}", directoryPath, e);
            return new OpenDirectoryResult(false, "打开目录失败: " + e.getMessage());
        }
    }

    @Override
    public String getAppRootPath() {
        return System.getProperty("user.dir");
    }

    @Override
    public OpenDirectoryResult openProjectLogDirectory(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            return new OpenDirectoryResult(false, "项目ID不能为空");
        }

        // 构建项目日志目录路径
        String appRootPath = getAppRootPath();
        String logPath = appRootPath + File.separator + "log" + File.separator + projectId;

        return openDirectory(logPath);
    }

    @Override
    public OpenDirectoryResult openExecutionOutputDirectory(String recordType, Long recordId) {
        if (recordType == null || recordType.trim().isEmpty()) {
            return new OpenDirectoryResult(false, "记录类型不能为空");
        }
        if (recordId == null) {
            return new OpenDirectoryResult(false, "记录ID不能为空");
        }

        String outputDir = null;

        // 根据记录类型从数据库查询输出目录
        if ("callgraph".equals(recordType)) {
            // 调用链记录
            CallGraphExecutionRecord record = templateExecutionRecordService.getCallGraphRecordById(recordId);
            if (record == null) {
                return new OpenDirectoryResult(false, "调用链执行记录不存在: " + recordId);
            }
            outputDir = record.getOutputDir();
        } else if ("findstack".equals(recordType)) {
            // 关键字生成堆栈记录
            FindStackExecutionRecord record = templateExecutionRecordService.getFindStackRecordById(recordId);
            if (record == null) {
                return new OpenDirectoryResult(false, "关键字生成堆栈执行记录不存在: " + recordId);
            }
            outputDir = record.getOutputDir();
        } else {
            return new OpenDirectoryResult(false, "不支持的记录类型: " + recordType);
        }

        // 检查输出目录是否存在
        if (outputDir == null || outputDir.trim().isEmpty()) {
            return new OpenDirectoryResult(false, "该执行记录没有输出目录");
        }

        return openDirectory(outputDir);
    }

    /**
     * 打开目录的结果
     */
    public static class OpenDirectoryResult {
        private final boolean success;
        private final String message;

        public OpenDirectoryResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
