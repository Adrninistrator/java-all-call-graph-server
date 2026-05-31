package com.github.adrninistrator.jacgserver.service.impl;

import com.adrninistrator.jacg.conf.ConfigureWrapper;
import com.adrninistrator.jacg.conf.enums.ConfigKeyEnum;
import com.adrninistrator.jacg.dto.callgraph.MethodCallGraphFilePathInfo;
import com.adrninistrator.jacg.dto.callstack.CallStackFileResult;
import com.adrninistrator.jacg.findstack.FindCallStackTrace;
import com.adrninistrator.jacg.runner.RunnerGenAllGraph4Callee;
import com.adrninistrator.jacg.runner.RunnerGenAllGraph4Caller;
import com.adrninistrator.jacg.runner.RunnerWriteDb;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.github.adrninistrator.jacgserver.constant.Constants;
import com.github.adrninistrator.jacgserver.exception.ConfigException;
import com.github.adrninistrator.jacgserver.exception.ExecuteException;
import com.github.adrninistrator.jacgserver.exception.ProjectNotFoundException;
import com.github.adrninistrator.jacgserver.exception.TemplateNotFoundException;
import com.github.adrninistrator.jacgserver.model.entity.AnalysisExecutionRecord;
import com.github.adrninistrator.jacgserver.model.entity.CallGraphExecutionRecord;
import com.github.adrninistrator.jacgserver.model.entity.CallGraphFileInfo;
import com.github.adrninistrator.jacgserver.model.entity.ExecutionRecord;
import com.github.adrninistrator.jacgserver.model.entity.FindStackExecutionRecord;
import com.github.adrninistrator.jacgserver.model.vo.ExecutionVO;
import com.github.adrninistrator.jacgserver.service.ConfigService;
import com.github.adrninistrator.jacgserver.service.ExecuteService;
import com.github.adrninistrator.jacgserver.service.ExecutionRecordService;
import com.github.adrninistrator.jacgserver.service.TemplateExecutionRecordService;
import com.github.adrninistrator.jacgserver.util.ExecutionLoggerManager;
import com.github.adrninistrator.jacgserver.util.FileUtil;
import com.github.adrninistrator.jacgserver.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 执行服务实现类
 * 
 * 直接从库配置文件读取配置参数执行，支持异步执行和执行状态追踪
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Service
public class ExecuteServiceImpl implements ExecuteService {

    private static final Logger logger = LoggerFactory.getLogger(ExecuteServiceImpl.class);

    /**
     * 执行记录存储（内存中）
     */
    private static final ConcurrentHashMap<String, ExecutionRecord> executionRecords = new ConcurrentHashMap<>();

    /**
     * 项目正在执行静态分析的标志（内存中）
     */
    private static final ConcurrentHashMap<String, String> projectExecutingFlags = new ConcurrentHashMap<>();

    /**
     * 模板正在执行调用链生成的标志（内存中）
     */
    private static final ConcurrentHashMap<String, String> templateExecutingFlags = new ConcurrentHashMap<>();

    /**
     * 异步执行线程池
     */
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    @Autowired
    private ConfigService configService;

    @Resource
    private ExecutionRecordService executionRecordService;

    @Resource
    private TemplateExecutionRecordService templateExecutionRecordService;

    @Override
    public ExecutionVO executeAnalysis(String projectId) {
        // 检查项目是否存在
        String projectDir = configService.getProjectConfDir() + File.separator + projectId;
        if (!FileUtil.exists(projectDir)) {
            throw new ProjectNotFoundException("项目不存在: " + projectId);
        }

        // 检查项目是否正在执行
        if (isProjectExecuting(projectId)) {
            throw new ExecuteException("项目正在执行静态分析，请稍后再试");
        }

        String execId = IdGenerator.generateId();

        // 设置项目执行标志
        projectExecutingFlags.put(projectId, execId);

        try {
            // 创建内存执行记录
            ExecutionRecord record = new ExecutionRecord();
            record.setExecId(execId);
            record.setProjectId(projectId);
            record.setType(Constants.EXEC_TYPE_ANALYSIS);
            record.setStatus(Constants.EXEC_STATUS_RUNNING);
            record.setStartTime(System.currentTimeMillis());

            executionRecords.put(execId, record);

            // 创建数据库执行记录
            AnalysisExecutionRecord dbRecord = new AnalysisExecutionRecord();
            dbRecord.setExecId(execId);
            dbRecord.setProjectId(projectId);

            // 读取项目描述和Jar文件列表
            try {
                String projectJsonPath = configService.getProjectConfDir() + File.separator + "project.json";
                String projectJson = FileUtil.readFile(projectJsonPath);
                if (projectJson != null) {
                    // 解析项目描述
                    String projectDesc = parseProjectDesc(projectJson, projectId);
                    dbRecord.setProjectDesc(projectDesc);
                }

                // 读取Jar文件列表
                String jarDirPath = projectDir + File.separator + "_javacg2_config" + File.separator + "jar_dir.properties";
                String jarFiles = FileUtil.readFile(jarDirPath);
                if (jarFiles != null) {
                    // 提取jar文件列表（去掉注释和空行）
                    StringBuilder jarListBuilder = new StringBuilder();
                    String[] lines = jarFiles.split("\n");
                    for (String line : lines) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            if (jarListBuilder.length() > 0) {
                                jarListBuilder.append("\n");
                            }
                            jarListBuilder.append(line);
                        }
                    }
                    dbRecord.setJarFiles(jarListBuilder.toString());
                }
            } catch (Exception e) {
                logger.warn("读取项目信息失败", e);
            }

            dbRecord.setStartTime(new Date());
            dbRecord.setStatus(Constants.EXEC_STATUS_RUNNING);

            // 保存到数据库
            executionRecordService.saveRecord(dbRecord);

            // 异步执行静态分析
            final Long dbRecordId = dbRecord.getId();
            final String logFilePath = normalizePath(Constants.LOG_DIR + File.separator + projectId + File.separator + "writeDb_" + execId + ".log");
            record.setLogFilePath(logFilePath);
            executorService.submit(() -> {
                doExecuteAnalysis(projectId, execId, record, dbRecordId, logFilePath);
            });

            return convertToVO(record);
        } catch (Exception e) {
            // 发生异常时清除执行标志
            projectExecutingFlags.remove(projectId);
            throw e;
        }
    }

    /**
     * 实际执行静态分析
     */
    private void doExecuteAnalysis(String projectId, String execId, ExecutionRecord record, Long dbRecordId, String logFilePath) {
        // 通过编程式配置创建动态日志Appender（日志文件按项目ID分目录，每次执行生成新文件）
        String logId = projectId + "_writeDb_" + execId;
        ExecutionLoggerManager.createLogger(logId, logFilePath);

        try {
            String projectDir = configService.getProjectConfDir() + File.separator + projectId;
            
            // 直接从项目目录的配置文件读取配置
            JavaCG2ConfigureWrapper javacg2Wrapper = new JavaCG2ConfigureWrapper(false, projectDir);
            ConfigureWrapper jacgWrapper = new ConfigureWrapper(false, projectDir);

            // 设置输出根目录
            String outputRootPath = configService.getOutputRootPath();
            jacgWrapper.setMainConfig(ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, outputRootPath);

            // 执行静态分析
            RunnerWriteDb runnerWriteDb = new RunnerWriteDb(javacg2Wrapper, jacgWrapper);
            boolean success = runnerWriteDb.run();

            // 计算执行耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - record.getStartTime();

            // 更新执行状态（内存）
            record.setStatus(success ? Constants.EXEC_STATUS_COMPLETED : Constants.EXEC_STATUS_FAILED);
            record.setEndTime(endTime);
            if (!success) {
                record.setErrorMessage("静态分析执行失败");
            }

            // 更新数据库状态
            executionRecordService.updateStatus(
                    dbRecordId,
                    success ? Constants.EXEC_STATUS_COMPLETED : Constants.EXEC_STATUS_FAILED,
                    duration,
                    success ? null : "静态分析执行失败",
                    logFilePath);

            logger.info("静态分析执行完成: projectId={}, execId={}, success={}, duration={}ms", projectId, execId, success, duration);

        } catch (Exception e) {
            logger.error("静态分析执行异常: projectId={}, execId={}", projectId, execId, e);
            long endTime = System.currentTimeMillis();
            long duration = endTime - record.getStartTime();
            record.setStatus(Constants.EXEC_STATUS_FAILED);
            record.setEndTime(endTime);
            record.setErrorMessage(e.getMessage());

            // 更新数据库状态
            executionRecordService.updateStatus(dbRecordId, Constants.EXEC_STATUS_FAILED, duration, e.getMessage(), logFilePath);
        } finally {
            // 移除日志Appender
            ExecutionLoggerManager.removeLogger(logId);
            
            // 删除项目执行标志
            projectExecutingFlags.remove(projectId);
        }
    }

    /**
     * 解析项目描述
     */
    private String parseProjectDesc(String projectJson, String projectId) {
        try {
            // 简单解析JSON，查找对应项目ID的描述
            if (projectJson.contains(projectId)) {
                int descIndex = projectJson.indexOf("\"description\"");
                if (descIndex > 0) {
                    int colonIndex = projectJson.indexOf(":", descIndex);
                    int quoteStart = projectJson.indexOf("\"", colonIndex);
                    int quoteEnd = projectJson.indexOf("\"", quoteStart + 1);
                    if (quoteStart > 0 && quoteEnd > quoteStart) {
                        return projectJson.substring(quoteStart + 1, quoteEnd);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("解析项目描述失败", e);
        }
        return null;
    }

    @Override
    public ExecutionVO executeCallGraph(String templateId) {
        // 查找模板目录
        String templateDir = findTemplateDir(templateId);
        if (templateDir == null) {
            throw new TemplateNotFoundException("模板不存在: " + templateId);
        }

        // 检查模板是否正在执行
        if (isTemplateExecuting(templateId)) {
            throw new ExecuteException("模板正在执行调用链生成，请稍后再试");
        }

        // 读取模板基本信息获取projectId和direction
        File templateInfoFile = new File(templateDir, "template.json");
        if (!templateInfoFile.exists()) {
            throw new ConfigException("模板配置文件不存在");
        }

        // 简单读取模板信息
        String projectId = readProjectIdFromTemplateInfo(templateInfoFile);
        String direction = readDirectionFromTemplateInfo(templateInfoFile);

        // 默认模板不支持通过页面执行
        if (isDefaultTemplate(templateInfoFile)) {
            throw new ExecuteException("默认模板不支持通过页面执行，请使用MCP工具执行");
        }

        // 检查项目是否正在执行
        if (isProjectExecuting(projectId)) {
            throw new ExecuteException("项目正在执行静态分析，请稍后再试");
        }

        // 检查项目是否有成功的执行记录
        AnalysisExecutionRecord latestSuccessRecord = executionRecordService.getLatestSuccessByProjectId(projectId);
        if (latestSuccessRecord == null) {
            throw new ExecuteException("当前项目没有成功的静态分析执行记录，请先执行静态分析");
        }

        String execId = IdGenerator.generateId();

        // 设置模板执行标志
        templateExecutingFlags.put(templateId, execId);

        try {
            // 创建执行记录
            ExecutionRecord record = new ExecutionRecord();
            record.setExecId(execId);
            record.setProjectId(projectId);
            record.setTemplateId(templateId);
            record.setType(Constants.EXEC_TYPE_CALLGRAPH);
            record.setStatus(Constants.EXEC_STATUS_RUNNING);
            record.setStartTime(System.currentTimeMillis());

            executionRecords.put(execId, record);

            // 读取入口方法列表
            String entryMethods = readEntryMethods(templateDir, direction);

            // 创建数据库执行记录
            CallGraphExecutionRecord dbRecord = new CallGraphExecutionRecord();
            dbRecord.setExecId(execId);
            dbRecord.setProjectId(projectId);
            dbRecord.setTemplateId(templateId);
            dbRecord.setDirection(direction);
            dbRecord.setEntryMethods(entryMethods);
            dbRecord.setStartTime(new Date());
            dbRecord.setStatus(Constants.EXEC_STATUS_RUNNING);
            templateExecutionRecordService.saveCallGraphRecord(dbRecord);

            final Long dbRecordId = dbRecord.getId();

            // 异步执行调用链生成
            executorService.submit(() -> {
                doExecuteCallGraph(templateId, projectId, direction, execId, record, dbRecordId);
            });

            return convertToVO(record);
        } catch (Exception e) {
            // 发生异常时清除执行标志
            templateExecutingFlags.remove(templateId);
            throw e;
        }
    }

    /**
     * 读取入口方法列表
     */
    private String readEntryMethods(String templateDir, String direction) {
        try {
            String configFileName = Constants.DIRECTION_CALLER.equals(direction) 
                    ? "_jacg_gen_all_call_graph/method_class_4caller.properties"
                    : "_jacg_gen_all_call_graph/method_class_4callee.properties";
            String filePath = templateDir + File.separator + configFileName;
            String content = FileUtil.readFile(filePath);
            if (content != null) {
                // 过滤注释和空行
                StringBuilder sb = new StringBuilder();
                for (String line : content.split("\n")) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append(line);
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            logger.warn("读取入口方法列表失败", e);
        }
        return null;
    }

    /**
     * 实际执行调用链生成
     */
    private void doExecuteCallGraph(String templateId, String projectId, String direction, String execId, ExecutionRecord record, Long dbRecordId) {
        // 通过编程式配置创建动态日志Appender（日志文件按项目ID分目录，每次执行生成新文件）
        String logId = projectId + "_" + execId;
        String logFilePath = normalizePath(Constants.LOG_DIR + File.separator + projectId + File.separator +
                "template_" + direction + "_" + templateId + "_" + execId + ".log");
        ExecutionLoggerManager.createLogger(logId, logFilePath);

        try {
            String templateDir = findTemplateDir(templateId);
            
            // 直接从模板目录的配置文件读取配置
            ConfigureWrapper wrapper = new ConfigureWrapper(false, templateDir);

            // 设置输出根目录
            String outputRootPath = configService.getOutputRootPath();
            wrapper.setMainConfig(ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, outputRootPath);

            // 固定设置
            wrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_WRITE_TO_FILE, "true");
            wrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_RETURN_IN_MEMORY, "false");

            boolean success;
            String outputDir = null;
            if (Constants.DIRECTION_CALLER.equals(direction)) {
                // 向下调用链
                RunnerGenAllGraph4Caller runner = new RunnerGenAllGraph4Caller(wrapper);
                success = runner.run();
                outputDir = normalizePath(runner.getCurrentOutputDirPath());
            } else {
                // 向上调用链
                RunnerGenAllGraph4Callee runner = new RunnerGenAllGraph4Callee(wrapper);
                success = runner.run();
                outputDir = normalizePath(runner.getCurrentOutputDirPath());
            }

            // 计算执行耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - record.getStartTime();

            // 更新执行状态（内存）
            record.setStatus(success ? Constants.EXEC_STATUS_COMPLETED : Constants.EXEC_STATUS_FAILED);
            record.setEndTime(endTime);
            record.setOutputDir(outputDir);
            if (!success) {
                record.setErrorMessage("调用链生成失败");
            }

            // 更新数据库状态
            templateExecutionRecordService.updateCallGraphStatus(
                    dbRecordId,
                    success ? Constants.EXEC_STATUS_COMPLETED : Constants.EXEC_STATUS_FAILED,
                    duration,
                    outputDir,
                    logFilePath,
                    success ? null : "调用链生成失败");

            logger.info("调用链生成完成: templateId={}, execId={}, success={}, outputDir={}", templateId, execId, success, outputDir);

        } catch (Exception e) {
            logger.error("调用链生成异常: templateId={}, execId={}", templateId, execId, e);
            long endTime = System.currentTimeMillis();
            long duration = endTime - record.getStartTime();
            record.setStatus(Constants.EXEC_STATUS_FAILED);
            record.setEndTime(endTime);
            record.setErrorMessage(e.getMessage());

            // 更新数据库状态
            templateExecutionRecordService.updateCallGraphStatus(
                    dbRecordId, Constants.EXEC_STATUS_FAILED, duration, null, logFilePath, e.getMessage());
        } finally {
            // 移除日志Appender
            ExecutionLoggerManager.removeLogger(logId);
            
            // 删除模板执行标志
            templateExecutingFlags.remove(templateId);
        }
    }

    @Override
    public ExecutionVO getExecutionStatus(String execId) {
        ExecutionRecord record = executionRecords.get(execId);
        if (record == null) {
            throw new ExecuteException("执行记录不存在: " + execId);
        }
        return convertToVO(record);
    }

    @Override
    public List<String> getExecutionLogs(String execId, int lines) {
        ExecutionRecord record = executionRecords.get(execId);
        if (record == null) {
            throw new ExecuteException("执行记录不存在: " + execId);
        }

        // 优先使用记录中的日志文件路径
        String logPath = record.getLogFilePath();
        if (logPath == null || logPath.isEmpty()) {
            // 兼容旧记录：使用旧命名规则
            if (Constants.EXEC_TYPE_ANALYSIS.equals(record.getType())) {
                logPath = Constants.LOG_DIR + File.separator + record.getProjectId() + File.separator + "writeDb.log";
            } else {
                logPath = Constants.LOG_DIR + File.separator + record.getProjectId() + File.separator + record.getExecId() + ".log";
            }
        }

        return FileUtil.readLastLines(logPath, lines > 0 ? lines : 100);
    }

    /**
     * 查找模板目录
     */
    private String findTemplateDir(String templateId) {
        String projectConfDir = configService.getProjectConfDir();
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
            if (templatesDir.exists() && templatesDir.isDirectory()) {
                File templateDir = new File(templatesDir, templateId);
                if (templateDir.exists() && templateDir.isDirectory()) {
                    return templateDir.getAbsolutePath();
                }
            }
        }

        return null;
    }

    /**
     * 从模板信息文件读取projectId
     */
    private String readProjectIdFromTemplateInfo(File templateInfoFile) {
        try {
            String content = FileUtil.readFile(templateInfoFile.getAbsolutePath());
            if (content == null) {
                return null;
            }
            // 简单解析JSON获取projectId
            int projectIdIndex = content.indexOf("\"projectId\"");
            if (projectIdIndex > 0) {
                int colonIndex = content.indexOf(":", projectIdIndex);
                int quoteStart = content.indexOf("\"", colonIndex);
                int quoteEnd = content.indexOf("\"", quoteStart + 1);
                return content.substring(quoteStart + 1, quoteEnd);
            }
        } catch (Exception e) {
            logger.warn("读取模板projectId失败", e);
        }
        return null;
    }

    /**
     * 从模板信息文件读取direction
     */
    private String readDirectionFromTemplateInfo(File templateInfoFile) {
        try {
            String content = FileUtil.readFile(templateInfoFile.getAbsolutePath());
            if (content == null) {
                return Constants.DIRECTION_CALLER;
            }
            // 简单解析JSON获取direction
            int directionIndex = content.indexOf("\"direction\"");
            if (directionIndex > 0) {
                int colonIndex = content.indexOf(":", directionIndex);
                int quoteStart = content.indexOf("\"", colonIndex);
                int quoteEnd = content.indexOf("\"", quoteStart + 1);
                return content.substring(quoteStart + 1, quoteEnd);
            }
        } catch (Exception e) {
            logger.warn("读取模板direction失败", e);
        }
        return Constants.DIRECTION_CALLER; // 默认向下
    }

    @Override
    public boolean isProjectExecuting(String projectId) {
        return projectExecutingFlags.containsKey(projectId);
    }

    @Override
    public boolean isTemplateExecuting(String templateId) {
        return templateExecutingFlags.containsKey(templateId);
    }

    @Override
    public ExecutionVO getProjectExecutionInfo(String projectId) {
        String execId = projectExecutingFlags.get(projectId);
        if (execId != null) {
            ExecutionRecord record = executionRecords.get(execId);
            if (record != null) {
                ExecutionVO vo = convertToVO(record);
                // 正在执行中，计算当前耗时
                vo.setDuration(System.currentTimeMillis() - record.getStartTime());
                return vo;
            }
        }
        // 查找最近完成的执行记录
        ExecutionRecord lastRecord = executionRecords.values().stream()
                .filter(r -> projectId.equals(r.getProjectId()) && Constants.EXEC_TYPE_ANALYSIS.equals(r.getType()))
                .filter(r -> r.getEndTime() > 0)
                .max((a, b) -> Long.compare(a.getEndTime(), b.getEndTime()))
                .orElse(null);
        return lastRecord != null ? convertToVO(lastRecord) : null;
    }

    @Override
    public ExecutionVO getTemplateExecutionInfo(String templateId) {
        String execId = templateExecutingFlags.get(templateId);
        if (execId != null) {
            ExecutionRecord record = executionRecords.get(execId);
            if (record != null) {
                ExecutionVO vo = convertToVO(record);
                // 正在执行中，计算当前耗时
                vo.setDuration(System.currentTimeMillis() - record.getStartTime());
                return vo;
            }
        }
        // 查找最近完成的执行记录（包括生成调用链和生成堆栈两种类型）
        ExecutionRecord lastRecord = executionRecords.values().stream()
                .filter(r -> templateId.equals(r.getTemplateId()) && 
                        (Constants.EXEC_TYPE_CALLGRAPH.equals(r.getType()) || Constants.EXEC_TYPE_FINDSTACK.equals(r.getType())))
                .filter(r -> r.getEndTime() > 0)
                .max((a, b) -> Long.compare(a.getEndTime(), b.getEndTime()))
                .orElse(null);
        return lastRecord != null ? convertToVO(lastRecord) : null;
    }

    /**
     * 转换为VO
     */
    private ExecutionVO convertToVO(ExecutionRecord record) {
        ExecutionVO vo = new ExecutionVO();
        vo.setExecId(record.getExecId());
        vo.setProjectId(record.getProjectId());
        vo.setTemplateId(record.getTemplateId());
        vo.setType(record.getType());
        vo.setStatus(record.getStatus());
        vo.setStartTime(IdGenerator.formatTime(record.getStartTime()));
        vo.setEndTime(record.getEndTime() > 0 ? IdGenerator.formatTime(record.getEndTime()) : null);
        vo.setOutputDir(record.getOutputDir());
        vo.setLogFilePath(record.getLogFilePath());
        vo.setErrorMessage(record.getErrorMessage());
        // 计算执行耗时
        if (record.getEndTime() > 0) {
            vo.setDuration(record.getEndTime() - record.getStartTime());
        }
        return vo;
    }

    @Override
    public ExecutionVO executeFindStack(String templateId) {
        // 查找模板目录
        String templateDir = findTemplateDir(templateId);
        if (templateDir == null) {
            throw new TemplateNotFoundException("模板不存在: " + templateId);
        }

        // 检查模板是否正在执行
        if (isTemplateExecuting(templateId)) {
            throw new ExecuteException("模板正在执行调用链生成，请稍后再试");
        }

        // 读取模板基本信息获取projectId和direction
        File templateInfoFile = new File(templateDir, "template.json");
        if (!templateInfoFile.exists()) {
            throw new ConfigException("模板配置文件不存在");
        }

        // 简单读取模板信息
        String projectId = readProjectIdFromTemplateInfo(templateInfoFile);
        String direction = readDirectionFromTemplateInfo(templateInfoFile);

        // 默认模板不支持通过页面执行
        if (isDefaultTemplate(templateInfoFile)) {
            throw new ExecuteException("默认模板不支持通过页面执行，请使用MCP工具执行");
        }

        // 检查项目是否正在执行
        if (isProjectExecuting(projectId)) {
            throw new ExecuteException("项目正在执行静态分析，请稍后再试");
        }

        // 检查项目是否有成功的执行记录
        AnalysisExecutionRecord latestSuccessRecord = executionRecordService.getLatestSuccessByProjectId(projectId);
        if (latestSuccessRecord == null) {
            throw new ExecuteException("当前项目没有成功的静态分析执行记录，请先执行静态分析");
        }

        String execId = IdGenerator.generateId();

        // 设置模板执行标志
        templateExecutingFlags.put(templateId, execId);

        try {
            // 创建执行记录
            ExecutionRecord record = new ExecutionRecord();
            record.setExecId(execId);
            record.setProjectId(projectId);
            record.setTemplateId(templateId);
            record.setType(Constants.EXEC_TYPE_FINDSTACK);
            record.setStatus(Constants.EXEC_STATUS_RUNNING);
            record.setStartTime(System.currentTimeMillis());

            executionRecords.put(execId, record);

            // 读取入口方法列表
            String entryMethods = readEntryMethods(templateDir, direction);
            // 读取关键字列表
            String keywords = readKeywords(templateDir, direction);

            // 检查关键字是否配置
            if (keywords == null || keywords.trim().isEmpty()) {
                throw new ExecuteException("未配置关键字，请先编辑模板配置关键字后再执行");
            }

            // 创建数据库执行记录
            FindStackExecutionRecord dbRecord = new FindStackExecutionRecord();
            dbRecord.setExecId(execId);
            dbRecord.setProjectId(projectId);
            dbRecord.setTemplateId(templateId);
            dbRecord.setDirection(direction);
            dbRecord.setEntryMethods(entryMethods);
            dbRecord.setKeywords(keywords);
            dbRecord.setStartTime(new Date());
            dbRecord.setStatus(Constants.EXEC_STATUS_RUNNING);
            templateExecutionRecordService.saveFindStackRecord(dbRecord);

            final Long dbRecordId = dbRecord.getId();

            // 异步执行生成调用链根据关键字生成堆栈
            executorService.submit(() -> {
                doExecuteFindStack(templateId, projectId, direction, execId, record, dbRecordId);
            });

            return convertToVO(record);
        } catch (Exception e) {
            // 发生异常时清除执行标志
            templateExecutingFlags.remove(templateId);
            throw e;
        }
    }

    /**
     * 读取关键字列表
     */
    private String readKeywords(String templateDir, String direction) {
        try {
            String configFileName = Constants.DIRECTION_CALLER.equals(direction)
                    ? "_jacg_find_stack_keyword/find_stack_keyword_4er.properties"
                    : "_jacg_find_stack_keyword/find_stack_keyword_4ee.properties";
            String filePath = templateDir + File.separator + configFileName;
            String content = FileUtil.readFile(filePath);
            if (content != null) {
                // 过滤注释和空行
                StringBuilder sb = new StringBuilder();
                for (String line : content.split("\n")) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append(line);
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            logger.warn("读取关键字列表失败", e);
        }
        return null;
    }

    /**
     * 实际执行根据关键字生成调用堆栈
     */
    private void doExecuteFindStack(String templateId, String projectId, String direction, String execId, ExecutionRecord record, Long dbRecordId) {
        // 通过编程式配置创建动态日志Appender（日志文件按项目ID分目录）
        String logId = projectId + "_findstack_" + execId;
        String logFilePath = normalizePath(Constants.LOG_DIR + File.separator + projectId + File.separator + execId + ".log");
        ExecutionLoggerManager.createLogger(logId, logFilePath);

        try {
            String templateDir = findTemplateDir(templateId);
            
            // 直接从模板目录的配置文件读取配置
            ConfigureWrapper wrapper = new ConfigureWrapper(false, templateDir);

            // 设置输出根目录
            String outputRootPath = configService.getOutputRootPath();
            wrapper.setMainConfig(ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, outputRootPath);

            // 根据调用链方向决定order4ee参数
            // order4ee: true-向上调用链(callee), false-向下调用链(caller)
            boolean order4ee = Constants.DIRECTION_CALLEE.equals(direction);

            // 执行根据关键字生成调用堆栈
            FindCallStackTrace findCallStackTrace = new FindCallStackTrace(order4ee, wrapper);
            CallStackFileResult result = findCallStackTrace.find();
            boolean success = result.isSuccess();
            String outputDir = normalizePath(findCallStackTrace.getCallGraphOutputDirPath());

            // 计算执行耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - record.getStartTime();

            // 更新执行状态（内存）
            record.setStatus(success ? Constants.EXEC_STATUS_COMPLETED : Constants.EXEC_STATUS_FAILED);
            record.setEndTime(endTime);
            record.setOutputDir(outputDir);
            if (!success) {
                record.setErrorMessage("生成调用链根据关键字生成堆栈失败");
            }

            // 更新数据库状态
            templateExecutionRecordService.updateFindStackStatus(
                    dbRecordId,
                    success ? Constants.EXEC_STATUS_COMPLETED : Constants.EXEC_STATUS_FAILED,
                    duration,
                    outputDir,
                    success ? null : "生成调用链根据关键字生成堆栈失败");

            logger.info("生成调用链根据关键字生成堆栈完成: templateId={}, execId={}, success={}, outputDir={}", templateId, execId, success, outputDir);

        } catch (Exception e) {
            logger.error("生成调用链根据关键字生成堆栈异常: templateId={}, execId={}", templateId, execId, e);
            long endTime = System.currentTimeMillis();
            long duration = endTime - record.getStartTime();
            record.setStatus(Constants.EXEC_STATUS_FAILED);
            record.setEndTime(endTime);
            record.setErrorMessage(e.getMessage());

            // 更新数据库状态
            templateExecutionRecordService.updateFindStackStatus(
                    dbRecordId, Constants.EXEC_STATUS_FAILED, duration, null, e.getMessage());
        } finally {
            // 移除日志Appender
            ExecutionLoggerManager.removeLogger(logId);
            
            // 删除模板执行标志
            templateExecutingFlags.remove(templateId);
        }
    }

    /**
     * 规范化路径，去除.、..等相对路径符号
     * 例如：D:\path\.\dir -> D:\path\dir
     * 
     * @param path 原始路径
     * @return 规范化后的路径，如果路径为空或处理失败则返回原路径
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        try {
            // 使用File.getCanonicalPath()规范化路径
            return new File(path).getCanonicalPath();
        } catch (Exception e) {
            logger.warn("路径规范化失败: {}, 错误: {}", path, e.getMessage());
            return path;
        }
    }

    /**
     * 检查模板是否为默认模板
     */
    private boolean isDefaultTemplate(File templateInfoFile) {
        try {
            String content = FileUtil.readFile(templateInfoFile.getAbsolutePath());
            if (content != null && content.contains("\"defaultTemplate\"")) {
                int index = content.indexOf("\"defaultTemplate\"");
                int colonIndex = content.indexOf(":", index);
                int valueEnd = content.indexOf(",", colonIndex);
                int valueEnd2 = content.indexOf("}", colonIndex);
                int end = (valueEnd2 < valueEnd || valueEnd == -1) ? valueEnd2 : valueEnd;
                if (end > colonIndex) {
                    String value = content.substring(colonIndex + 1, end).trim();
                    return "true".equalsIgnoreCase(value);
                }
            }
        } catch (Exception e) {
            logger.warn("读取模板默认标记失败", e);
        }
        return false;
    }

    /**
     * 执行默认模板（供MCP工具调用，异步执行）
     * 根据项目ID和方向找到默认模板，使用传入的入口类/方法替换配置后执行
     *
     * @param projectId 项目ID
     * @param direction 调用链方向：caller(向下), callee(向上)
     * @param entryMethods 入口类/方法列表
     * @return 执行信息（包含执行ID）
     */
    public ExecutionVO executeDefaultTemplate(String projectId, String direction, List<String> entryMethods) {
        // 检查项目是否存在
        String projectDir = configService.getProjectConfDir() + File.separator + projectId;
        if (!FileUtil.exists(projectDir)) {
            throw new ProjectNotFoundException("项目不存在: " + projectId);
        }

        // 检查项目是否正在执行
        if (isProjectExecuting(projectId)) {
            throw new ExecuteException("项目正在执行静态分析，请稍后再试");
        }

        // 检查项目是否有成功的执行记录
        AnalysisExecutionRecord latestSuccessRecord = executionRecordService.getLatestSuccessByProjectId(projectId);
        if (latestSuccessRecord == null) {
            throw new ExecuteException("当前项目没有成功的静态分析执行记录，请先执行静态分析");
        }

        // 查找默认模板目录
        String defaultTemplateDir = findDefaultTemplateDir(projectId, direction);
        if (defaultTemplateDir == null) {
            throw new ConfigException("当前项目不存在" + (Constants.DIRECTION_CALLEE.equals(direction) ? "向上" : "向下") + "默认模板");
        }

        // 检查默认模板是否正在执行
        String defaultTemplateId = getDefaultTemplateId(projectId, direction);
        if (defaultTemplateId != null && isTemplateExecuting(defaultTemplateId)) {
            throw new ExecuteException("默认模板正在执行，请稍后再试");
        }

        // 检查入口方法列表
        if (entryMethods == null || entryMethods.isEmpty()) {
            throw new ConfigException("入口类/方法不能为空");
        }

        String execId = IdGenerator.generateId();

        // 设置模板执行标志
        if (defaultTemplateId != null) {
            templateExecutingFlags.put(defaultTemplateId, execId);
        }

        try {
            // 创建执行记录
            ExecutionRecord record = new ExecutionRecord();
            record.setExecId(execId);
            record.setProjectId(projectId);
            record.setTemplateId(defaultTemplateId);
            record.setType(Constants.EXEC_TYPE_CALLGRAPH);
            record.setStatus(Constants.EXEC_STATUS_RUNNING);
            record.setStartTime(System.currentTimeMillis());
            executionRecords.put(execId, record);

            // 创建数据库执行记录
            CallGraphExecutionRecord dbRecord = new CallGraphExecutionRecord();
            dbRecord.setExecId(execId);
            dbRecord.setProjectId(projectId);
            dbRecord.setTemplateId(defaultTemplateId);
            dbRecord.setDirection(direction);
            dbRecord.setEntryMethods(String.join("\n", entryMethods));
            dbRecord.setStartTime(new Date());
            dbRecord.setStatus(Constants.EXEC_STATUS_RUNNING);
            templateExecutionRecordService.saveCallGraphRecord(dbRecord);

            final Long dbRecordId = dbRecord.getId();
            final String finalDefaultTemplateId = defaultTemplateId;

            // 预先计算日志文件路径并设置到 record，使同步返回值包含 logFilePath
            final String logFilePath = normalizePath(Constants.LOG_DIR + File.separator + projectId + File.separator +
                    "template_" + direction + "_" + defaultTemplateId + "_" + execId + ".log");
            record.setLogFilePath(logFilePath);

            // 异步执行默认模板
            executorService.submit(() -> {
                doExecuteDefaultTemplate(defaultTemplateDir, projectId, direction, entryMethods, execId, record, dbRecordId, finalDefaultTemplateId);
            });

            return convertToVO(record);

        } catch (Exception e) {
            // 发生异常时清除执行标志
            if (defaultTemplateId != null) {
                templateExecutingFlags.remove(defaultTemplateId);
            }
            throw e;
        }
    }

    /**
     * 实际执行默认模板（异步执行）
     */
    private void doExecuteDefaultTemplate(String defaultTemplateDir, String projectId, String direction,
                                           List<String> entryMethods, String execId, ExecutionRecord record,
                                           Long dbRecordId, String defaultTemplateId) {
        // 通过编程式配置创建动态日志Appender（每次执行生成新文件）
        String logId = projectId + "_default_" + execId;
        String logFilePath = normalizePath(Constants.LOG_DIR + File.separator + projectId + File.separator +
                "template_" + direction + "_" + defaultTemplateId + "_" + execId + ".log");
        record.setLogFilePath(logFilePath);
        ExecutionLoggerManager.createLogger(logId, logFilePath);

        try {
            // 从默认模板目录读取配置
            ConfigureWrapper wrapper = new ConfigureWrapper(false, defaultTemplateDir);

            // 设置输出根目录
            String outputRootPath = configService.getOutputRootPath();
            wrapper.setMainConfig(ConfigKeyEnum.CKE_OUTPUT_ROOT_PATH, outputRootPath);

            // 固定设置
            wrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_WRITE_TO_FILE, "true");
            wrapper.setMainConfig(ConfigKeyEnum.CKE_CALL_GRAPH_RETURN_IN_MEMORY, "false");

            // 替换入口类/方法配置
            com.adrninistrator.jacg.conf.enums.OtherConfigFileUseSetEnum entryPointsKey = Constants.DIRECTION_CALLER.equals(direction)
                    ? com.adrninistrator.jacg.conf.enums.OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLER
                    : com.adrninistrator.jacg.conf.enums.OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLEE;
            wrapper.setOtherConfigSet(entryPointsKey, new java.util.LinkedHashSet<>(entryMethods));

            boolean success;
            String outputDir = null;
            Map<String, MethodCallGraphFilePathInfo> methodCallGraphFilePathMap = null;

            if (Constants.DIRECTION_CALLER.equals(direction)) {
                RunnerGenAllGraph4Caller runner = new RunnerGenAllGraph4Caller(wrapper);
                success = runner.run();
                outputDir = normalizePath(runner.getCurrentOutputDirPath());
                methodCallGraphFilePathMap = runner.getMethodCallGraphFilePathMap();
            } else {
                RunnerGenAllGraph4Callee runner = new RunnerGenAllGraph4Callee(wrapper);
                success = runner.run();
                outputDir = normalizePath(runner.getCurrentOutputDirPath());
                methodCallGraphFilePathMap = runner.getMethodCallGraphFilePathMap();
            }

            // 计算执行耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - record.getStartTime();

            // 当执行失败时，从日志文件获取更具体的错误信息
            String specificError = null;
            if (!success) {
                specificError = readErrorFromLogFile(logFilePath);
            }
            String errorMessage = specificError != null ? "默认模板调用链生成失败: " + specificError : "默认模板调用链生成失败";

            // 更新执行状态（内存）
            record.setStatus(success ? Constants.EXEC_STATUS_COMPLETED : Constants.EXEC_STATUS_FAILED);
            record.setEndTime(endTime);
            record.setOutputDir(outputDir);
            if (!success) {
                record.setErrorMessage(errorMessage);
            }

            // 将methodCallGraphFilePathMap保存到新表call_graph_file_info
            if (methodCallGraphFilePathMap != null && !methodCallGraphFilePathMap.isEmpty() && success) {
                saveCallGraphFileInfoList(dbRecordId, methodCallGraphFilePathMap);
            }

            // 更新数据库状态
            templateExecutionRecordService.updateCallGraphStatus(
                    dbRecordId,
                    success ? Constants.EXEC_STATUS_COMPLETED : Constants.EXEC_STATUS_FAILED,
                    duration,
                    outputDir,
                    logFilePath,
                    success ? null : errorMessage);

            logger.info("默认模板执行完成: projectId={}, direction={}, execId={}, success={}, outputDir={}", projectId, direction, execId, success, outputDir);

        } catch (Exception e) {
            logger.error("默认模板执行异常: projectId={}, direction={}, execId={}", projectId, direction, execId, e);
            long endTime = System.currentTimeMillis();
            long duration = endTime - record.getStartTime();
            record.setStatus(Constants.EXEC_STATUS_FAILED);
            record.setEndTime(endTime);
            record.setErrorMessage(e.getMessage());

            // 更新数据库状态
            templateExecutionRecordService.updateCallGraphStatus(
                    dbRecordId, Constants.EXEC_STATUS_FAILED, duration, null, logFilePath, e.getMessage());
        } finally {
            // 移除日志Appender
            ExecutionLoggerManager.removeLogger(logId);

            // 删除模板执行标志
            if (defaultTemplateId != null) {
                templateExecutingFlags.remove(defaultTemplateId);
            }
        }
    }

    /**
     * 将方法调用链文件路径Map保存到call_graph_file_info表
     *
     * @param recordId 调用链执行记录ID
     * @param methodCallGraphFilePathMap 方法调用链文件路径Map
     */
    private void saveCallGraphFileInfoList(Long recordId, Map<String, MethodCallGraphFilePathInfo> methodCallGraphFilePathMap) {
        java.util.List<CallGraphFileInfo> fileInfoList = new java.util.ArrayList<>();
        for (Map.Entry<String, MethodCallGraphFilePathInfo> entry : methodCallGraphFilePathMap.entrySet()) {
            CallGraphFileInfo fileInfo = new CallGraphFileInfo();
            fileInfo.setRecordId(recordId);
            fileInfo.setEntryMethod(entry.getKey());
            fileInfo.setOrigText(entry.getValue().getOrigText());
            fileInfo.setFilePath(entry.getValue().getFilePath());
            fileInfoList.add(fileInfo);
        }
        templateExecutionRecordService.saveCallGraphFileInfoList(recordId, fileInfoList);
    }

    /**
     * 查找项目指定方向的默认模板ID
     *
     * @param projectId 项目ID
     * @param direction 调用链方向
     * @return 默认模板ID，不存在则返回null
     */
    public String getDefaultTemplateId(String projectId, String direction) {
        String templatesDir = configService.getProjectConfDir() + File.separator + projectId + File.separator + Constants.TEMPLATES_DIR;
        File templatesDirFile = new File(templatesDir);

        if (!templatesDirFile.exists() || !templatesDirFile.isDirectory()) {
            return null;
        }

        File[] templateDirs = templatesDirFile.listFiles(File::isDirectory);
        if (templateDirs == null) {
            return null;
        }

        for (File templateDir : templateDirs) {
            File templateInfoFile = new File(templateDir, "template.json");
            if (templateInfoFile.exists()) {
                String content = FileUtil.readFile(templateInfoFile.getAbsolutePath());
                if (content != null) {
                    // 检查是否为默认模板
                    if (content.contains("\"defaultTemplate\"") && isDefaultTemplate(templateInfoFile)) {
                        // 检查方向是否匹配
                        String templateDirection = readDirectionFromTemplateInfo(templateInfoFile);
                        if (direction.equals(templateDirection)) {
                            return templateDir.getName();
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 查找项目指定方向的默认模板目录
     *
     * @param projectId 项目ID
     * @param direction 调用链方向
     * @return 默认模板目录路径，不存在则返回null
     */
    private String findDefaultTemplateDir(String projectId, String direction) {
        String defaultTemplateId = getDefaultTemplateId(projectId, direction);
        if (defaultTemplateId != null) {
            return findTemplateDir(defaultTemplateId);
        }
        return null;
    }

    /**
     * 从日志文件中读取错误信息
     * 当默认模板执行失败时，从日志文件中提取具体的ERROR信息，便于定位问题原因
     *
     * @param logFilePath 日志文件路径
     * @return 错误信息，无错误时返回null
     */
    private String readErrorFromLogFile(String logFilePath) {
        if (logFilePath == null) {
            return null;
        }
        try {
            File logFile = new File(logFilePath);
            if (!logFile.exists() || !logFile.isFile()) {
                return null;
            }
            List<String> lines = java.nio.file.Files.readAllLines(logFile.toPath());
            List<String> errorLines = new java.util.ArrayList<>();
            for (String line : lines) {
                if (line.contains(" ERROR ") || line.contains(" 预检查失败 ") || line.contains(" 预处理失败 ") || line.contains(" 执行失败 ")) {
                    // 去掉行首的时间戳等前缀，保留关键错误信息
                    String trimmed = line.trim();
                    if (trimmed.length() > 200) {
                        trimmed = trimmed.substring(0, 200) + "...";
                    }
                    errorLines.add(trimmed);
                }
                if (errorLines.size() >= 5) {
                    break;
                }
            }
            if (!errorLines.isEmpty()) {
                return String.join("; ", errorLines);
            }
            return null;
        } catch (Exception e) {
            logger.warn("读取日志文件失败: {}", logFilePath, e);
            return null;
        }
    }
}
