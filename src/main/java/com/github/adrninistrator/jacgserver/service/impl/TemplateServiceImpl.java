package com.github.adrninistrator.jacgserver.service.impl;

import com.adrninistrator.jacg.conf.ConfigureWrapper;
import com.adrninistrator.jacg.conf.enums.ConfigDbKeyEnum;
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
import java.util.List;

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
    public TemplateVO getTemplate(String templateId) {
        String templateDir = findTemplateDir(templateId);
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
        // 检查模板名称是否为空
        if (templateDTO.getDescription() == null || templateDTO.getDescription().trim().isEmpty()) {
            throw new ConfigException("模板名称不能为空");
        }

        // 检查模板名称是否重复（在同一项目内）
        if (isTemplateNameExists(projectId, templateDTO.getDescription(), null)) {
            throw new ConfigException("模板名称已存在: " + templateDTO.getDescription());
        }

        // 检查入口类/方法是否为空
        if (!hasEntryPoints(templateDTO.getJacgConfig(), templateDTO.getDirection())) {
            throw new ConfigException("入口类/方法不能为空");
        }

        // 检查关键字配置：如果启用了生成调用链根据关键字生成堆栈功能，关键字参数不能为空
        validateFindStackKeywords(templateDTO.getJacgConfig(), templateDTO.getDirection());

        String templateId = IdGenerator.generateId();
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
        templateInfo.setDescription(templateDTO.getDescription());
        templateInfo.setDirection(templateDTO.getDirection());
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
        templateVO.setDescription(templateDTO.getDescription());
        templateVO.setDirection(templateDTO.getDirection());
        templateVO.setCreateTime(currentTime);
        templateVO.setUpdateTime(currentTime);
        templateVO.setJacgConfig(jacgConfig);

        logger.info("创建模板成功: templateId={}, projectId={}", templateId, projectId);
        return templateVO;
    }

    @Override
    public void updateTemplate(String templateId, TemplateDTO templateDTO) {
        // 检查模板是否正在执行
        if (executeService.isTemplateExecuting(templateId)) {
            throw new ExecuteException("模板正在执行调用链生成，请稍后再试");
        }

        String templateDir = findTemplateDir(templateId);
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

        // 检查模板名称是否为空
        if (templateDTO.getDescription() == null || templateDTO.getDescription().trim().isEmpty()) {
            throw new ConfigException("模板名称不能为空");
        }

        // 检查模板名称是否重复（在同一项目内，排除当前模板）
        if (isTemplateNameExists(existingInfo.getProjectId(), templateDTO.getDescription(), templateId)) {
            throw new ConfigException("模板名称已存在: " + templateDTO.getDescription());
        }

        // 检查入口类/方法是否为空
        if (!hasEntryPoints(templateDTO.getJacgConfig(), templateDTO.getDirection())) {
            throw new ConfigException("入口类/方法不能为空");
        }

        // 检查关键字配置：如果启用了生成调用链根据关键字生成堆栈功能，关键字参数不能为空
        validateFindStackKeywords(templateDTO.getJacgConfig(), templateDTO.getDirection());

        String currentTime = IdGenerator.getCurrentTime();

        // 更新基本信息
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
    public void deleteTemplate(String templateId) {
        // 检查模板是否正在执行
        if (executeService.isTemplateExecuting(templateId)) {
            throw new ExecuteException("模板正在执行调用链生成，请稍后再试");
        }

        String templateDir = findTemplateDir(templateId);
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
    public TemplateVO copyTemplate(String templateId, String description) {
        // 检查模板是否正在执行
        if (executeService.isTemplateExecuting(templateId)) {
            throw new ExecuteException("模板正在执行调用链生成，请稍后再试");
        }

        TemplateVO sourceTemplate = getTemplate(templateId);

        // 检查项目是否正在执行
        if (executeService.isProjectExecuting(sourceTemplate.getProjectId())) {
            throw new ExecuteException("项目正在执行静态分析，请稍后再试");
        }

        TemplateDTO templateDTO = new TemplateDTO();
        templateDTO.setDescription(description != null ? description : sourceTemplate.getDescription() + "-副本");
        templateDTO.setDirection(sourceTemplate.getDirection());
        templateDTO.setJacgConfig(sourceTemplate.getJacgConfig());

        return createTemplate(sourceTemplate.getProjectId(), templateDTO);
    }

    /**
     * 构建模板的ConfigureWrapper（用于执行调用链生成）
     * 直接从配置文件读取，不从DTO构建
     */
    public ConfigureWrapper buildConfigureWrapper(String templateId) {
        String templateDir = findTemplateDir(templateId);
        if (templateDir == null) {
            throw new TemplateNotFoundException("模板不存在: " + templateId);
        }

        // 直接从配置文件读取，创建ConfigureWrapper
        ConfigureWrapper wrapper = new ConfigureWrapper(false, templateDir);
        return wrapper;
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
     * 检查模板名称是否已存在（在同一项目内）
     *
     * @param projectId 项目ID
     * @param description 模板名称
     * @param excludeTemplateId 排除的模板ID（更新时使用）
     * @return true-已存在，false-不存在
     */
    private boolean isTemplateNameExists(String projectId, String description, String excludeTemplateId) {
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
        String configKey = "caller".equals(direction) ? "OCFUSE_METHOD_CLASS_4CALLER" : "OCFUSE_METHOD_CLASS_4CALLEE";
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
        String keywordConfigKey = "caller".equals(direction) ? "OCFULE_FIND_STACK_KEYWORD_4ER" : "OCFULE_FIND_STACK_KEYWORD_4EE";
        List<String> keywords = jacgConfig.getListConfig().get(keywordConfigKey);

        // 如果配置了关键字，检查是否为空
        if (keywords != null && keywords.isEmpty()) {
            throw new ConfigException("启用生成调用链根据关键字生成堆栈功能时，关键字参数不能为空");
        }
    }
}
