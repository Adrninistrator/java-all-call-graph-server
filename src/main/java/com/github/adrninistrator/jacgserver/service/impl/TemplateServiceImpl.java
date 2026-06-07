package com.github.adrninistrator.jacgserver.service.impl;


import com.adrninistrator.jacg.conf.enums.ConfigDbKeyEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseListEnum;
import com.adrninistrator.jacg.conf.enums.OtherConfigFileUseSetEnum;
import com.github.adrninistrator.jacgserver.constant.Constants;
import com.github.adrninistrator.jacgserver.exception.ConfigException;
import com.github.adrninistrator.jacgserver.exception.ExecuteException;
import com.github.adrninistrator.jacgserver.exception.ProjectNotFoundException;
import com.github.adrninistrator.jacgserver.exception.TemplateNotFoundException;
import com.github.adrninistrator.jacgserver.model.dto.JACGConfigDTO;
import com.github.adrninistrator.jacgserver.model.dto.TemplateDTO;
import com.github.adrninistrator.jacgserver.model.entity.TemplateInfoEntity;
import com.github.adrninistrator.jacgserver.model.vo.TemplateVO;
import com.github.adrninistrator.jacgserver.service.ConfigService;
import com.github.adrninistrator.jacgserver.service.ExecuteService;
import com.github.adrninistrator.jacgserver.service.TemplateService;
import com.github.adrninistrator.jacgserver.util.ConfigReaderUtil;
import com.github.adrninistrator.jacgserver.util.ConfigWriterUtil;
import com.github.adrninistrator.jacgserver.util.ExecutionLoggerManager;
import com.github.adrninistrator.jacgserver.util.FileUtil;
import com.github.adrninistrator.jacgserver.util.IdGenerator;
import com.github.adrninistrator.jacgserver.util.JsonUtil;
import com.github.adrninistrator.jacgserver.util.MDCUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模板管理服务实现类
 * 
 * 配置参数直接使用java-all-call-graph库的标准配置文件格式，
 * 不再使用额外的template_config.json文件
 * 模板基本信息保存在template.json文件中
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Service
public class TemplateServiceImpl implements TemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateServiceImpl.class);

    private static final String TEMPLATE_INFO_FILE = "template.json";

    /**
     * 模板写入锁池，防止同一模板并发保存时的 read-modify-write 竞态条件导致配置丢失
     */
    private final ConcurrentHashMap<String, Object> templateWriteLocks = new ConcurrentHashMap<>();

    @Autowired
    private ConfigService configService;

    @Autowired
    private ExecuteService executeService;

    @Override
    public List<TemplateVO> listTemplates(String projectId) {
        String templatesDir = configService.getProjectConfDir() + File.separator + projectId + File.separator + Constants.TEMPLATES_DIR;
        File templatesDirFile = new File(templatesDir);

        List<TemplateVO> templateVOList = new ArrayList<>();
        if (!templatesDirFile.exists() || !templatesDirFile.isDirectory()) {
            return templateVOList;
        }

        File[] templateDirs = templatesDirFile.listFiles(File::isDirectory);
        if (templateDirs == null) {
            return templateVOList;
        }

        for (File templateDir : templateDirs) {
            File templateInfoFile = new File(templateDir, TEMPLATE_INFO_FILE);
            if (templateInfoFile.exists()) {
                TemplateInfoEntity infoEntity = JsonUtil.fromFile(templateInfoFile, TemplateInfoEntity.class);
                if (infoEntity != null) {
                    TemplateVO templateVO = new TemplateVO();
                    templateVO.setTemplateId(infoEntity.getTemplateId());
                    templateVO.setProjectId(infoEntity.getProjectId());
                    templateVO.setDescription(infoEntity.getDescription());
                    templateVO.setDirection(infoEntity.getDirection());
                    templateVO.setDefaultTemplate(infoEntity.getDefaultTemplate() != null ? infoEntity.getDefaultTemplate() : false);
                    templateVO.setCreateTime(infoEntity.getCreateTime());
                    templateVO.setUpdateTime(infoEntity.getUpdateTime());
                    templateVOList.add(templateVO);
                }
            }
        }

        // 按创建时间倒序排序
        templateVOList.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return templateVOList;
    }

    @Override
    public TemplateVO getTemplate(String projectId, String templateId) {
        String templateDir = configService.findTemplateDir(templateId, projectId);
        if (templateDir == null) {
            throw new TemplateNotFoundException("模板不存在: " + templateId);
        }

        File templateInfoFile = new File(templateDir, TEMPLATE_INFO_FILE);
        if (!templateInfoFile.exists()) {
            throw new TemplateNotFoundException("模板配置文件不存在: " + templateId);
        }

        TemplateInfoEntity infoEntity = JsonUtil.fromFile(templateInfoFile, TemplateInfoEntity.class);
        if (infoEntity == null) {
            throw new ConfigException("读取模板配置失败");
        }

        TemplateVO templateVO = new TemplateVO();
        templateVO.setTemplateId(infoEntity.getTemplateId());
        templateVO.setProjectId(infoEntity.getProjectId());
        templateVO.setDescription(infoEntity.getDescription());
        templateVO.setDirection(infoEntity.getDirection());
        templateVO.setDefaultTemplate(infoEntity.getDefaultTemplate() != null ? infoEntity.getDefaultTemplate() : false);
        templateVO.setCreateTime(infoEntity.getCreateTime());
        templateVO.setUpdateTime(infoEntity.getUpdateTime());

        // 从库配置文件读取配置参数
        JACGConfigDTO jacgConfig = ConfigReaderUtil.readJACGConfig(templateDir);
        templateVO.setJacgConfig(jacgConfig);

        return templateVO;
    }

    @Override
    public TemplateVO createTemplate(String projectId, TemplateDTO templateDTO) {
        // 检查项目是否正在执行
        if (executeService.isProjectExecuting(projectId)) {
            throw new ExecuteException("项目正在执行静态分析，请稍后再试");
        }

        // 检查项目是否存在
        String projectDir = configService.getProjectConfDir() + File.separator + projectId;
        if (!FileUtil.exists(projectDir)) {
            throw new ProjectNotFoundException("项目不存在: " + projectId);
        }

        // 创建动态日志Appender，使库日志写入项目对应的日志目录
        String logId = projectId + "_template_create";
        String logFilePath = Constants.LOG_DIR + File.separator + projectId + File.separator + "template_create.log";
        ExecutionLoggerManager.createLogger(logId, logFilePath);

        // 设置动态日志上下文
        MDCUtil.setTemplateCreateMDC(projectId);
        try {
            return doCreateTemplate(projectId, templateDTO);
        } finally {
            MDCUtil.clearMDC();
            ExecutionLoggerManager.removeLogger(logId);
        }
    }

    /**
     * 实际创建模板的逻辑
     */
    private TemplateVO doCreateTemplate(String projectId, TemplateDTO templateDTO) {
        boolean isDefaultTemplate = Boolean.TRUE.equals(templateDTO.getDefaultTemplate());
        String direction = templateDTO.getDirection();

        // 默认模板使用固定的模板描述与ID
        String templateDesc;
        String templateId;
        if (isDefaultTemplate) {
            templateDesc = Constants.DIRECTION_CALLEE.equals(direction)
                    ? Constants.DEFAULT_TEMPLATE_DESC_4EE
                    : Constants.DEFAULT_TEMPLATE_DESC_4ER;
            templateId = templateDesc;
            // 覆盖description为固定描述
            templateDTO.setDescription(templateDesc);
        } else {
            templateDesc = templateDTO.getDescription();
            templateId = IdGenerator.generateId();
        }

        // 检查模板描述是否为空
        if (templateDesc == null || templateDesc.trim().isEmpty()) {
            throw new ConfigException("模板描述不能为空");
        }

        // 检查模板描述是否重复（在同一项目内）
        if (isTemplateDescExists(projectId, templateDesc, null)) {
            throw new ConfigException("模板描述已存在: " + templateDesc);
        }

        // 默认模板检查：每个项目每个方向只能有一个默认模板
        if (isDefaultTemplate) {
            if (isDefaultTemplateExists(projectId, direction)) {
                throw new ConfigException("当前项目已存在" + (Constants.DIRECTION_CALLEE.equals(direction) ? "向上" : "向下") + "默认模板");
            }
        }

        // 默认模板使用占位符作为入口类/方法，跳过非空校验
        if (isDefaultTemplate) {
            setDefaultTemplateEntryPlaceholder(templateDTO, direction);
        } else {
            // 非默认模板检查入口类/方法是否为空
            if (!hasEntryPoints(templateDTO.getJacgConfig(), direction)) {
                throw new ConfigException("入口类/方法不能为空");
            }
        }

        // 检查关键字配置：如果启用了生成调用链根据关键字生成堆栈功能，关键字参数不能为空
        validateFindStackKeywords(templateDTO.getJacgConfig(), direction);

        // 模板不允许指定数据库配置参数，模板使用项目的数据库配置
        // 前端不应发送dbConfig参数，若发送则忽略，后端会自动使用项目的数据库配置覆盖
        if (templateDTO.getJacgConfig() != null) {
            templateDTO.getJacgConfig().setDbConfig(null);
        }

        String currentTime = IdGenerator.getCurrentTime();

        // 创建模板目录
        String projectDir = configService.getProjectConfDir() + File.separator + projectId;
        String templatesDir = projectDir + File.separator + Constants.TEMPLATES_DIR;
        String templateDir = templatesDir + File.separator + templateId;
        if (!FileUtil.createDirectory(templateDir)) {
            throw new ConfigException("创建模板目录失败: " + templateDir);
        }

        // 保存模板基本信息
        TemplateInfoEntity templateInfo = new TemplateInfoEntity();
        templateInfo.setTemplateId(templateId);
        templateInfo.setProjectId(projectId);
        templateInfo.setDescription(templateDesc);
        templateInfo.setDirection(direction);
        templateInfo.setDefaultTemplate(isDefaultTemplate);
        templateInfo.setCreateTime(currentTime);
        templateInfo.setUpdateTime(currentTime);

        File templateInfoFile = new File(templateDir, TEMPLATE_INFO_FILE);
        if (!JsonUtil.toFile(templateInfoFile, templateInfo)) {
            throw new ConfigException("保存模板配置失败");
        }

        // 使用项目的数据库配置（项目与模板使用相同的数据库配置）
        JACGConfigDTO jacgConfig = templateDTO.getJacgConfig();
        if (jacgConfig == null) {
            jacgConfig = new JACGConfigDTO();
        }
        // 读取项目的数据库配置并覆盖模板的数据库配置
        JACGConfigDTO projectJacgConfig = ConfigReaderUtil.readJACGConfig(projectDir);
        jacgConfig.setDbConfig(projectJacgConfig.getDbConfig());

        // 生成配置文件（使用库的标准格式）
        ConfigWriterUtil.writeJACGConfig(templateDir, jacgConfig);

        // 复制项目的数据库配置文件到模板目录（覆盖）
        copyDbConfigFile(projectDir, templateDir);

        TemplateVO templateVO = new TemplateVO();
        templateVO.setTemplateId(templateId);
        templateVO.setProjectId(projectId);
        templateVO.setDescription(templateDesc);
        templateVO.setDirection(direction);
        templateVO.setDefaultTemplate(isDefaultTemplate);
        templateVO.setCreateTime(currentTime);
        templateVO.setUpdateTime(currentTime);
        templateVO.setJacgConfig(jacgConfig);

        logger.info("创建模板成功: templateId={}, projectId={}, defaultTemplate={}", templateId, projectId, isDefaultTemplate);
        return templateVO;
    }

    @Override
    public void updateTemplate(String projectId, String templateId, TemplateDTO templateDTO) {
        // 检查模板是否正在执行
        if (executeService.isTemplateExecuting(templateId)) {
            throw new ExecuteException("模板正在执行调用链生成，请稍后再试");
        }

        // 对同一模板的保存操作加锁，防止并发 read-modify-write 竞态导致配置丢失
        Object lock = templateWriteLocks.computeIfAbsent(templateId, k -> new Object());
        synchronized (lock) {
            doUpdateTemplate(projectId, templateId, templateDTO);
        }
    }

    @Override
    public void mergeUpdateTemplate(String projectId, String templateId, TemplateDTO templateDTO) {
        // 检查模板是否正在执行
        if (executeService.isTemplateExecuting(templateId)) {
            throw new ExecuteException("模板正在执行调用链生成，请稍后再试");
        }

        // 对同一模板的保存操作加锁，在锁内完成 Read-Merge-Write 防止并发覆盖
        Object lock = templateWriteLocks.computeIfAbsent(templateId, k -> new Object());
        synchronized (lock) {
            doMergeUpdateTemplate(projectId, templateId, templateDTO);
        }
    }

    /**
     * 在锁内执行：读取当前完整配置 → 合并增量 → 校验 → 写入
     */
    private void doMergeUpdateTemplate(String projectId, String templateId, TemplateDTO incrementalDTO) {
        String templateDir = configService.findTemplateDir(templateId, projectId);
        if (templateDir == null) {
            throw new TemplateNotFoundException("模板不存在: " + templateId);
        }

        File templateInfoFile = new File(templateDir, TEMPLATE_INFO_FILE);
        TemplateInfoEntity existingInfo = JsonUtil.fromFile(templateInfoFile, TemplateInfoEntity.class);
        if (existingInfo == null) {
            throw new ConfigException("读取模板配置失败");
        }

        // 读取当前磁盘上的完整配置文件
        JACGConfigDTO currentConfig = ConfigReaderUtil.readJACGConfig(templateDir);

        // 合并增量配置到当前配置：仅覆盖传入的非空类别
        JACGConfigDTO mergedConfig = mergeJACGConfig(currentConfig, incrementalDTO.getJacgConfig());

        // 构建完整的 TemplateDTO（基本信息 + 合并后的配置）
        TemplateDTO fullDTO = new TemplateDTO();
        fullDTO.setDescription(incrementalDTO.getDescription() != null ? incrementalDTO.getDescription() : existingInfo.getDescription());
        fullDTO.setDirection(incrementalDTO.getDirection() != null ? incrementalDTO.getDirection() : existingInfo.getDirection());
        fullDTO.setDefaultTemplate(incrementalDTO.getDefaultTemplate() != null ? incrementalDTO.getDefaultTemplate() : existingInfo.getDefaultTemplate());
        fullDTO.setJacgConfig(mergedConfig);

        // 委托给 doUpdateTemplate 完成校验和写入
        doUpdateTemplate(projectId, templateId, fullDTO);
    }

    /**
     * 合并增量 JACG 配置到当前配置
     * 在每个配置类别内做 key 级别合并：保留当前配置中未修改的 key，仅覆盖增量中指定的 key
     */
    private JACGConfigDTO mergeJACGConfig(JACGConfigDTO current, JACGConfigDTO incremental) {
        if (current == null) {
            return incremental == null ? new JACGConfigDTO() : incremental;
        }
        if (incremental == null) {
            return current;
        }

        JACGConfigDTO merged = new JACGConfigDTO();

        // dbConfig 由项目配置决定，不在此合并
        merged.setDbConfig(current.getDbConfig());

        // mainConfig: key 级别合并，增量的 key 覆盖当前值
        if (incremental.getMainConfig() != null) {
            Map<String, Object> mainMerged = current.getMainConfig() != null ?
                    new HashMap<>(current.getMainConfig()) : new HashMap<>();
            mainMerged.putAll(incremental.getMainConfig());
            merged.setMainConfig(mainMerged);
        } else {
            merged.setMainConfig(current.getMainConfig());
        }

        // listConfig: key 级别合并，增量的 key 整体替换当前值（列表不支持追加，整体替换）
        if (incremental.getListConfig() != null) {
            Map<String, List<String>> listMerged = current.getListConfig() != null ?
                    new HashMap<>(current.getListConfig()) : new HashMap<>();
            listMerged.putAll(incremental.getListConfig());
            merged.setListConfig(listMerged);
        } else {
            merged.setListConfig(current.getListConfig());
        }

        // setConfig: key 级别合并，增量的 key 整体替换当前值
        if (incremental.getSetConfig() != null) {
            Map<String, List<String>> setMerged = current.getSetConfig() != null ?
                    new HashMap<>(current.getSetConfig()) : new HashMap<>();
            setMerged.putAll(incremental.getSetConfig());
            merged.setSetConfig(setMerged);
        } else {
            merged.setSetConfig(current.getSetConfig());
        }

        // elConfig: key 级别合并，增量的 key 覆盖当前值
        if (incremental.getElConfig() != null) {
            Map<String, Object> elMerged = current.getElConfig() != null ?
                    new HashMap<>(current.getElConfig()) : new HashMap<>();
            elMerged.putAll(incremental.getElConfig());
            merged.setElConfig(elMerged);
        } else {
            merged.setElConfig(current.getElConfig());
        }

        return merged;
    }

    private void doUpdateTemplate(String projectId, String templateId, TemplateDTO templateDTO) {

        String templateDir = configService.findTemplateDir(templateId, projectId);
        if (templateDir == null) {
            throw new TemplateNotFoundException("模板不存在: " + templateId);
        }

        File templateInfoFile = new File(templateDir, TEMPLATE_INFO_FILE);
        TemplateInfoEntity existingInfo = JsonUtil.fromFile(templateInfoFile, TemplateInfoEntity.class);
        if (existingInfo == null) {
            throw new ConfigException("读取模板配置失败");
        }

        // 检查项目是否正在执行
        if (executeService.isProjectExecuting(existingInfo.getProjectId())) {
            throw new ExecuteException("项目正在执行静态分析，请稍后再试");
        }

        // 检查模板描述是否为空
        if (templateDTO.getDescription() == null || templateDTO.getDescription().trim().isEmpty()) {
            throw new ConfigException("模板描述不能为空");
        }

        // 默认模板不允许修改模板描述
        boolean isExistingDefaultTemplate = Boolean.TRUE.equals(existingInfo.getDefaultTemplate());
        if (isExistingDefaultTemplate) {
            // 默认模板保持原有描述和方向，不允许修改
            templateDTO.setDescription(existingInfo.getDescription());
            templateDTO.setDirection(existingInfo.getDirection());
        }

        // 检查模板描述是否重复（在同一项目内，排除当前模板）
        if (isTemplateDescExists(existingInfo.getProjectId(), templateDTO.getDescription(), templateId)) {
            throw new ConfigException("模板描述已存在: " + templateDTO.getDescription());
        }

        // 检查入口类/方法是否为空（默认模板不检查，使用占位符）
        if (!isExistingDefaultTemplate && !hasEntryPoints(templateDTO.getJacgConfig(), templateDTO.getDirection())) {
            throw new ConfigException("入口类/方法不能为空");
        }

        // 默认模板不允许修改入口类/方法配置，检查并拒绝修改
        if (isExistingDefaultTemplate && isDefaultTemplateEntryPointModified(templateDTO, existingInfo.getDirection())) {
            throw new ConfigException("默认模板的入口类/方法配置不允许修改");
        }

        // 模板不允许修改数据库配置参数，模板使用项目的数据库配置
        // 前端不应发送dbConfig参数，若发送则忽略，后端会自动使用项目的数据库配置覆盖
        if (templateDTO.getJacgConfig() != null) {
            templateDTO.getJacgConfig().setDbConfig(null);
        }

        // 检查关键字配置：如果启用了生成调用链根据关键字生成堆栈功能，关键字参数不能为空
        validateFindStackKeywords(templateDTO.getJacgConfig(), templateDTO.getDirection());

        String currentTime = IdGenerator.getCurrentTime();

        // 更新基本信息（默认模板保持原有名称和方向）
        existingInfo.setDescription(templateDTO.getDescription());
        existingInfo.setDirection(templateDTO.getDirection());
        existingInfo.setUpdateTime(currentTime);

        if (!JsonUtil.toFile(templateInfoFile, existingInfo)) {
            throw new ConfigException("保存模板配置失败");
        }

        // 使用项目的数据库配置（项目与模板使用相同的数据库配置）
        JACGConfigDTO jacgConfig = templateDTO.getJacgConfig();
        if (jacgConfig == null) {
            jacgConfig = new JACGConfigDTO();
        }
        // 读取项目的数据库配置并覆盖模板的数据库配置
        String projectDir = configService.getProjectConfDir() + File.separator + existingInfo.getProjectId();
        JACGConfigDTO projectJacgConfig = ConfigReaderUtil.readJACGConfig(projectDir);
        jacgConfig.setDbConfig(projectJacgConfig.getDbConfig());

        // 重新生成配置文件
        ConfigWriterUtil.writeJACGConfig(templateDir, jacgConfig);

        // 复制项目的数据库配置文件到模板目录（覆盖）
        copyDbConfigFile(projectDir, templateDir);

        logger.info("更新模板成功: templateId={}", templateId);
    }

    /**
     * 复制项目的数据库配置文件到模板目录
     * 项目与模板使用完全相同的数据库配置，直接复制文件确保一致性
     *
     * @param projectDir 项目目录路径
     * @param templateDir 模板目录路径
     */
    private void copyDbConfigFile(String projectDir, String templateDir) {
        // 获取数据库配置文件名（相对于配置目录）
        String dbConfigFileName = ConfigDbKeyEnum.CDKE_DB_USE_H2.getFileName();

        // 构建源文件和目标文件路径
        File sourceFile = new File(projectDir, dbConfigFileName);
        File targetFile = new File(templateDir, dbConfigFileName);

        if (!sourceFile.exists()) {
            logger.warn("项目的数据库配置文件不存在，跳过复制: {}", sourceFile.getAbsolutePath());
            return;
        }

        // 确保目标目录存在
        File targetParentDir = targetFile.getParentFile();
        if (!targetParentDir.exists()) {
            if (!targetParentDir.mkdirs()) {
                throw new ConfigException("创建模板数据库配置目录失败: " + targetParentDir.getAbsolutePath());
            }
        }

        // 复制文件
        try {
            Path sourcePath = sourceFile.toPath();
            Path targetPath = targetFile.toPath();
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("复制数据库配置文件成功: {} -> {}", sourcePath, targetPath);
        } catch (IOException e) {
            throw new ConfigException("复制数据库配置文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteTemplate(String projectId, String templateId) {
        // 检查模板是否正在执行
        if (executeService.isTemplateExecuting(templateId)) {
            throw new ExecuteException("模板正在执行调用链生成，请稍后再试");
        }

        String templateDir = configService.findTemplateDir(templateId, projectId);
        if (templateDir == null) {
            throw new TemplateNotFoundException("模板不存在: " + templateId);
        }

        // 获取模板所属项目ID，检查项目是否正在执行
        File templateInfoFile = new File(templateDir, TEMPLATE_INFO_FILE);
        TemplateInfoEntity infoEntity = JsonUtil.fromFile(templateInfoFile, TemplateInfoEntity.class);
        if (infoEntity != null && executeService.isProjectExecuting(infoEntity.getProjectId())) {
            throw new ExecuteException("项目正在执行静态分析，请稍后再试");
        }

        if (!FileUtil.deleteDirectory(templateDir)) {
            throw new ConfigException("删除模板目录失败");
        }

        logger.info("删除模板成功: templateId={}", templateId);
    }

    @Override
    public TemplateVO copyTemplate(String projectId, String templateId, String description) {
        // 检查模板是否正在执行
        if (executeService.isTemplateExecuting(templateId)) {
            throw new ExecuteException("模板正在执行调用链生成，请稍后再试");
        }

        TemplateVO sourceTemplate = getTemplate(projectId, templateId);

        // 检查项目是否正在执行
        if (executeService.isProjectExecuting(sourceTemplate.getProjectId())) {
            throw new ExecuteException("项目正在执行静态分析，请稍后再试");
        }

        TemplateDTO templateDTO = new TemplateDTO();
        templateDTO.setDescription(description != null ? description : sourceTemplate.getDescription() + "-副本");
        templateDTO.setDirection(sourceTemplate.getDirection());
        templateDTO.setJacgConfig(sourceTemplate.getJacgConfig());
        // 复制模板时清除数据库配置，模板使用项目的数据库配置
        templateDTO.getJacgConfig().setDbConfig(null);
        // 复制模板时不复制默认模板标记
        templateDTO.setDefaultTemplate(false);

        return createTemplate(sourceTemplate.getProjectId(), templateDTO);
    }


    /**
     * 检查模板描述是否已存在（在同一项目内）
     *
     * @param projectId 项目ID
     * @param description 模板描述
     * @param excludeTemplateId 排除的模板ID（更新时使用）
     * @return true-已存在，false-不存在
     */
    private boolean isTemplateDescExists(String projectId, String description, String excludeTemplateId) {
        if (description == null || description.isEmpty()) {
            return false;
        }

        String templatesDir = configService.getProjectConfDir() + File.separator + projectId + File.separator + Constants.TEMPLATES_DIR;
        File templatesDirFile = new File(templatesDir);

        if (!templatesDirFile.exists() || !templatesDirFile.isDirectory()) {
            return false;
        }

        File[] templateDirs = templatesDirFile.listFiles(File::isDirectory);
        if (templateDirs == null) {
            return false;
        }

        for (File templateDir : templateDirs) {
            File templateInfoFile = new File(templateDir, TEMPLATE_INFO_FILE);
            if (templateInfoFile.exists()) {
                TemplateInfoEntity infoEntity = JsonUtil.fromFile(templateInfoFile, TemplateInfoEntity.class);
                if (infoEntity != null && description.equals(infoEntity.getDescription())) {
                    // 如果是更新操作，排除当前模板
                    if (excludeTemplateId != null && excludeTemplateId.equals(infoEntity.getTemplateId())) {
                        continue;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查JACG配置中是否有入口类/方法配置
     *
     * @param jacgConfig JACG配置
     * @param direction 调用链方向
     * @return true-有配置，false-无配置
     */
    private boolean hasEntryPoints(JACGConfigDTO jacgConfig, String direction) {
        if (jacgConfig == null || jacgConfig.getSetConfig() == null) {
            return false;
        }
        String configKey = "caller".equals(direction) ? OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLER.name() : OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLEE.name();
        List<String> entryPoints = jacgConfig.getSetConfig().get(configKey);
        return entryPoints != null && !entryPoints.isEmpty();
    }

    /**
     * 验证关键字配置：如果启用了生成调用链根据关键字生成堆栈功能，关键字参数不能为空
     *
     * @param jacgConfig JACG配置
     * @param direction 调用链方向
     */
    private void validateFindStackKeywords(JACGConfigDTO jacgConfig, String direction) {
        if (jacgConfig == null || jacgConfig.getListConfig() == null) {
            return;
        }

        // 根据调用链方向确定关键字配置的key
        String keywordConfigKey = "caller".equals(direction) ? OtherConfigFileUseListEnum.OCFULE_FIND_STACK_KEYWORD_4ER.name() : OtherConfigFileUseListEnum.OCFULE_FIND_STACK_KEYWORD_4EE.name();
        List<String> keywords = jacgConfig.getListConfig().get(keywordConfigKey);

        // 如果配置了关键字，检查是否为空
        if (keywords != null && keywords.isEmpty()) {
            throw new ConfigException("启用生成调用链根据关键字生成堆栈功能时，关键字参数不能为空");
        }
    }

    /**
     * 检查项目是否已存在指定方向的默认模板
     *
     * @param projectId 项目ID
     * @param direction 调用链方向
     * @return true-已存在，false-不存在
     */
    private boolean isDefaultTemplateExists(String projectId, String direction) {
        String templatesDir = configService.getProjectConfDir() + File.separator + projectId + File.separator + Constants.TEMPLATES_DIR;
        File templatesDirFile = new File(templatesDir);

        if (!templatesDirFile.exists() || !templatesDirFile.isDirectory()) {
            return false;
        }

        File[] templateDirs = templatesDirFile.listFiles(File::isDirectory);
        if (templateDirs == null) {
            return false;
        }

        for (File templateDir : templateDirs) {
            File templateInfoFile = new File(templateDir, TEMPLATE_INFO_FILE);
            if (templateInfoFile.exists()) {
                TemplateInfoEntity infoEntity = JsonUtil.fromFile(templateInfoFile, TemplateInfoEntity.class);
                if (infoEntity != null && Boolean.TRUE.equals(infoEntity.getDefaultTemplate())
                        && direction.equals(infoEntity.getDirection())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断默认模板的入口类/方法配置是否被修改
     * 默认模板的入口类/方法配置使用固定占位符 ${place_holder}，不允许修改为其他值
     *
     * @param templateDTO 模板DTO
     * @param direction 调用链方向
     * @return true-已修改（不允许），false-未修改（允许）
     */
    private boolean isDefaultTemplateEntryPointModified(TemplateDTO templateDTO, String direction) {
        JACGConfigDTO jacgConfig = templateDTO.getJacgConfig();
        if (jacgConfig == null || jacgConfig.getSetConfig() == null) {
            return false;
        }
        String entryPointsKey = Constants.DIRECTION_CALLER.equals(direction)
                ? OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLER.name() : OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLEE.name();
        List<String> entryPoints = jacgConfig.getSetConfig().get(entryPointsKey);
        if (entryPoints == null || entryPoints.isEmpty()) {
            return false;
        }
        // 判断入口类/方法是否不是占位符
        return entryPoints.size() != 1 || !Constants.DEFAULT_TEMPLATE_ENTRY_PLACEHOLDER.equals(entryPoints.get(0));
    }

    /**
     * 为默认模板设置入口类/方法占位符
     * 默认模板的入口类/方法使用固定占位符 ${place_holder}，执行时由MCP工具指定实际的入口类/方法替换
     *
     * @param templateDTO 模板DTO
     * @param direction 调用链方向
     */
    private void setDefaultTemplateEntryPlaceholder(TemplateDTO templateDTO, String direction) {
        JACGConfigDTO jacgConfig = templateDTO.getJacgConfig();
        if (jacgConfig == null) {
            jacgConfig = new JACGConfigDTO();
            templateDTO.setJacgConfig(jacgConfig);
        }
        if (jacgConfig.getSetConfig() == null) {
            jacgConfig.setSetConfig(new java.util.HashMap<>());
        }
        String entryPointsKey = Constants.DIRECTION_CALLER.equals(direction)
                ? OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLER.name() : OtherConfigFileUseSetEnum.OCFUSE_METHOD_CLASS_4CALLEE.name();
        List<String> placeholderList = new ArrayList<>();
        placeholderList.add(Constants.DEFAULT_TEMPLATE_ENTRY_PLACEHOLDER);
        jacgConfig.getSetConfig().put(entryPointsKey, placeholderList);
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
            File templateInfoFile = new File(templateDir, TEMPLATE_INFO_FILE);
            if (templateInfoFile.exists()) {
                TemplateInfoEntity infoEntity = JsonUtil.fromFile(templateInfoFile, TemplateInfoEntity.class);
                if (infoEntity != null && Boolean.TRUE.equals(infoEntity.getDefaultTemplate())
                        && direction.equals(infoEntity.getDirection())) {
                    return infoEntity.getTemplateId();
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
    public String findDefaultTemplateDir(String projectId, String direction) {
        String defaultTemplateId = getDefaultTemplateId(projectId, direction);
        if (defaultTemplateId != null) {
            return configService.findTemplateDir(defaultTemplateId, projectId);
        }
        return null;
    }
}
