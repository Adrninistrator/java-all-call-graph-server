package com.github.adrninistrator.jacgserver.service.impl;

import com.adrninistrator.jacg.conf.enums.ConfigDbKeyEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.github.adrninistrator.jacgserver.constant.Constants;
import com.github.adrninistrator.jacgserver.exception.ConfigException;
import com.github.adrninistrator.jacgserver.exception.ExecuteException;
import com.github.adrninistrator.jacgserver.exception.ProjectNotFoundException;
import com.github.adrninistrator.jacgserver.model.dto.JACGConfigDTO;
import com.github.adrninistrator.jacgserver.model.dto.JavaCG2ConfigDTO;
import com.github.adrninistrator.jacgserver.model.dto.ProjectDTO;
import com.github.adrninistrator.jacgserver.model.entity.ProjectListEntity;
import com.github.adrninistrator.jacgserver.model.vo.ProjectVO;
import com.github.adrninistrator.jacgserver.service.ConfigService;
import com.github.adrninistrator.jacgserver.service.ExecuteService;
import com.github.adrninistrator.jacgserver.service.ProjectService;
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

/**
 * 项目管理服务实现类
 * 
 * 配置参数直接使用java-callgraph2、java-all-call-graph库的标准配置文件格式，
 * 不再使用额外的project_config.json文件
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Service
public class ProjectServiceImpl implements ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    /**
     * 项目写入锁池，防止同一项目并发保存时的 read-modify-write 竞态条件导致配置丢失
     */
    private final java.util.concurrent.ConcurrentHashMap<String, Object> projectWriteLocks = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    private ConfigService configService;

    @Autowired
    private ExecuteService executeService;

    @Override
    public List<ProjectVO> listProjects() {
        String projectJsonPath = configService.getProjectConfDir() + File.separator + Constants.PROJECT_JSON_FILE;
        File projectJsonFile = new File(projectJsonPath);

        List<ProjectVO> projectVOList = new ArrayList<>();
        if (!projectJsonFile.exists()) {
            return projectVOList;
        }

        ProjectListEntity projectListEntity = JsonUtil.fromFile(projectJsonFile, ProjectListEntity.class);
        if (projectListEntity == null || projectListEntity.getProjects() == null) {
            return projectVOList;
        }

        for (ProjectListEntity.ProjectListItem item : projectListEntity.getProjects()) {
            ProjectVO projectVO = new ProjectVO();
            projectVO.setProjectId(item.getProjectId());
            projectVO.setDescription(item.getDescription());
            projectVO.setProjectRootDir(item.getProjectRootDir());
                projectVO.setCreateTime(item.getCreateTime());
                projectVO.setUpdateTime(item.getUpdateTime());
                projectVO.setCreatedByMcp(item.isCreatedByMcp());
            projectVOList.add(projectVO);
        }

        // 按创建时间倒序排序
        projectVOList.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return projectVOList;
    }

    @Override
    public ProjectVO getProject(String projectId) {
        String projectDir = configService.getProjectConfDir() + File.separator + projectId;
        if (!FileUtil.exists(projectDir)) {
            throw new ProjectNotFoundException("项目不存在: " + projectId);
        }

        // 从project.json获取基本信息
        String projectJsonPath = configService.getProjectConfDir() + File.separator + Constants.PROJECT_JSON_FILE;
        File projectJsonFile = new File(projectJsonPath);
        ProjectListEntity projectListEntity = JsonUtil.fromFile(projectJsonFile, ProjectListEntity.class);

        ProjectVO projectVO = new ProjectVO();
        projectVO.setProjectId(projectId);

        if (projectListEntity != null && projectListEntity.getProjects() != null) {
            for (ProjectListEntity.ProjectListItem item : projectListEntity.getProjects()) {
                if (projectId.equals(item.getProjectId())) {
                    projectVO.setDescription(item.getDescription());
                    projectVO.setProjectRootDir(item.getProjectRootDir());
                    projectVO.setCreateTime(item.getCreateTime());
                    projectVO.setUpdateTime(item.getUpdateTime());
                    projectVO.setCreatedByMcp(item.isCreatedByMcp());
                    break;
                }
            }
        }

        // 从库配置文件读取配置参数
        JavaCG2ConfigDTO javacg2Config = ConfigReaderUtil.readJavaCG2Config(projectDir);
        JACGConfigDTO jacgConfig = ConfigReaderUtil.readJACGConfig(projectDir);

        projectVO.setJavacg2Config(javacg2Config);
        projectVO.setJacgConfig(jacgConfig);

        return projectVO;
    }

    @Override
    public ProjectVO createProject(ProjectDTO projectDTO) {
        // 检查项目描述是否为空
        if (projectDTO.getDescription() == null || projectDTO.getDescription().trim().isEmpty()) {
            throw new ConfigException("项目描述不能为空");
        }

        // 最早阶段生成项目ID，用于日志目录
        String projectId = IdGenerator.generateId();

        // 创建动态日志Appender，使库日志写入项目对应的日志目录
        String logId = projectId + "_project_create";
        String logFilePath = Constants.LOG_DIR + File.separator + projectId + File.separator + "project_create.log";
        ExecutionLoggerManager.createLogger(logId, logFilePath);

        // 设置动态日志上下文
        MDCUtil.setProjectCreateMDC(projectId);
        try {
            return doCreateProject(projectId, projectDTO);
        } finally {
            MDCUtil.clearMDC();
            ExecutionLoggerManager.removeLogger(logId);
        }
    }

    /**
     * 实际创建项目的逻辑
     */
    private ProjectVO doCreateProject(String projectId, ProjectDTO projectDTO) {
        // 检查项目描述是否重复
        if (isProjectDescExists(projectDTO.getDescription(), null)) {
            throw new ConfigException("项目描述已存在: " + projectDTO.getDescription());
        }

        // 规范化项目根目录
        String canonicalProjectRootDir = canonicalizeProjectRootDir(projectDTO.getProjectRootDir());

        // 检查项目根目录是否重复
        if (canonicalProjectRootDir != null && isProjectRootDirExists(canonicalProjectRootDir, null)) {
            ProjectListEntity.ProjectListItem existingItem = findProjectByRootDir(canonicalProjectRootDir);
            String existInfo = existingItem != null ? "，已存在项目: " + existingItem.getDescription() + "（ID: " + existingItem.getProjectId() + "）" : "";
            throw new ConfigException("项目根目录已存在: " + canonicalProjectRootDir + existInfo);
        }

        // 检查Jar/Class文件路径是否为空
        if (!hasJarPaths(projectDTO.getJavacg2Config())) {
            throw new ConfigException("Jar/Class文件路径不能为空");
        }

        String currentTime = IdGenerator.getCurrentTime();

        // 创建项目目录
        String projectDir = configService.getProjectConfDir() + File.separator + projectId;
        if (!FileUtil.createDirectory(projectDir)) {
            throw new ConfigException("创建项目目录失败: " + projectDir);
        }

        // 创建模板目录
        String templatesDir = projectDir + File.separator + Constants.TEMPLATES_DIR;
        if (!FileUtil.createDirectory(templatesDir)) {
            throw new ConfigException("创建模板目录失败: " + templatesDir);
        }

        // 生成配置文件（使用库的标准格式）
        if (projectDTO.getJavacg2Config() != null) {
            ConfigWriterUtil.writeJavaCG2Config(projectDir, projectDTO.getJavacg2Config());
        }
        if (projectDTO.getJacgConfig() != null) {
            ConfigWriterUtil.writeJACGConfig(projectDir, projectDTO.getJacgConfig());
        }

        // 更新项目列表
        boolean createdByMcp = projectDTO.getCreatedByMcp() != null && projectDTO.getCreatedByMcp();
        updateProjectList(projectId, projectDTO.getDescription(), canonicalProjectRootDir, currentTime, currentTime, false, createdByMcp);

        ProjectVO projectVO = new ProjectVO();
        projectVO.setProjectId(projectId);
        projectVO.setDescription(projectDTO.getDescription());
        projectVO.setProjectRootDir(canonicalProjectRootDir);
        projectVO.setCreateTime(currentTime);
        projectVO.setUpdateTime(currentTime);
        projectVO.setCreatedByMcp(createdByMcp);
        projectVO.setJavacg2Config(projectDTO.getJavacg2Config());
        projectVO.setJacgConfig(projectDTO.getJacgConfig());

        logger.info("创建项目成功: projectId={}, description={}", projectId, projectDTO.getDescription());
        return projectVO;
    }

    @Override
    public void updateProject(String projectId, ProjectDTO projectDTO) {
        // 检查项目是否正在执行
        if (executeService.isProjectExecuting(projectId)) {
            throw new ExecuteException("项目正在执行静态分析，请稍后再试");
        }

        // 对同一项目的保存操作加锁，防止并发 read-modify-write 竞态导致配置丢失
        Object lock = projectWriteLocks.computeIfAbsent(projectId, k -> new Object());
        synchronized (lock) {
            doUpdateProject(projectId, projectDTO);
        }
    }

    private void doUpdateProject(String projectId, ProjectDTO projectDTO) {

        String projectDir = configService.getProjectConfDir() + File.separator + projectId;
        if (!FileUtil.exists(projectDir)) {
            throw new ProjectNotFoundException("项目不存在: " + projectId);
        }

        // 检查项目描述是否为空
        if (projectDTO.getDescription() == null || projectDTO.getDescription().trim().isEmpty()) {
            throw new ConfigException("项目描述不能为空");
        }

        // 检查项目描述是否重复（排除当前项目）
        if (isProjectDescExists(projectDTO.getDescription(), projectId)) {
            throw new ConfigException("项目描述已存在: " + projectDTO.getDescription());
        }

        // 规范化项目根目录
        String canonicalProjectRootDir = canonicalizeProjectRootDir(projectDTO.getProjectRootDir());

        // 检查项目根目录是否重复（排除当前项目）
        if (canonicalProjectRootDir != null && isProjectRootDirExists(canonicalProjectRootDir, projectId)) {
            ProjectListEntity.ProjectListItem existingItem = findProjectByRootDir(canonicalProjectRootDir);
            String existInfo = existingItem != null ? "，已存在项目: " + existingItem.getDescription() + "（ID: " + existingItem.getProjectId() + "）" : "";
            throw new ConfigException("项目根目录已存在: " + canonicalProjectRootDir + existInfo);
        }

        // 检查Jar/Class文件路径是否为空
        if (!hasJarPaths(projectDTO.getJavacg2Config())) {
            throw new ConfigException("Jar/Class文件路径不能为空");
        }

        String currentTime = IdGenerator.getCurrentTime();

        // 从project.json获取创建时间
        String projectJsonPath = configService.getProjectConfDir() + File.separator + Constants.PROJECT_JSON_FILE;
        File projectJsonFile = new File(projectJsonPath);
        String createTime = currentTime;
        
        if (projectJsonFile.exists()) {
            ProjectListEntity projectListEntity = JsonUtil.fromFile(projectJsonFile, ProjectListEntity.class);
            if (projectListEntity != null && projectListEntity.getProjects() != null) {
                for (ProjectListEntity.ProjectListItem item : projectListEntity.getProjects()) {
                    if (projectId.equals(item.getProjectId())) {
                        createTime = item.getCreateTime();
                        break;
                    }
                }
            }
        }

        // 重新生成配置文件
        if (projectDTO.getJavacg2Config() != null) {
            ConfigWriterUtil.writeJavaCG2Config(projectDir, projectDTO.getJavacg2Config());
        }
        if (projectDTO.getJacgConfig() != null) {
            ConfigWriterUtil.writeJACGConfig(projectDir, projectDTO.getJacgConfig());
        }

        // 复制项目的数据库配置文件到所有模板目录（项目与模板使用完全相同的数据库配置）
        copyDbConfigFileToAllTemplates(projectDir);

        // 更新项目列表（更新时不修改createdByMcp）
        updateProjectList(projectId, projectDTO.getDescription(), canonicalProjectRootDir, createTime, currentTime, true, false);

        logger.info("更新项目成功: projectId={}", projectId);
    }

    /**
     * 复制项目的数据库配置文件到所有模板目录
     * 项目与项目下的所有模板都使用完全相同的数据库配置
     *
     * @param projectDir 项目目录路径
     */
    private void copyDbConfigFileToAllTemplates(String projectDir) {
        // 获取数据库配置文件名（相对于配置目录）
        String dbConfigFileName = ConfigDbKeyEnum.CDKE_DB_USE_H2.getFileName();
        File sourceFile = new File(projectDir, dbConfigFileName);

        if (!sourceFile.exists()) {
            logger.warn("项目的数据库配置文件不存在，跳过复制到模板: {}", sourceFile.getAbsolutePath());
            return;
        }

        // 获取项目下的所有模板目录
        String templatesDir = projectDir + File.separator + Constants.TEMPLATES_DIR;
        File templatesDirFile = new File(templatesDir);
        if (!templatesDirFile.exists() || !templatesDirFile.isDirectory()) {
            logger.debug("项目下没有模板目录: {}", templatesDir);
            return;
        }

        File[] templateDirs = templatesDirFile.listFiles(File::isDirectory);
        if (templateDirs == null || templateDirs.length == 0) {
            logger.debug("项目下没有模板: {}", templatesDir);
            return;
        }

        // 遍历所有模板目录，复制数据库配置文件
        for (File templateDir : templateDirs) {
            File targetFile = new File(templateDir, dbConfigFileName);
            File targetParentDir = targetFile.getParentFile();

            // 确保目标目录存在
            if (!targetParentDir.exists()) {
                if (!targetParentDir.mkdirs()) {
                    logger.warn("创建模板数据库配置目录失败: {}", targetParentDir.getAbsolutePath());
                    continue;
                }
            }

            // 复制文件
            try {
                Path sourcePath = sourceFile.toPath();
                Path targetPath = targetFile.toPath();
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("复制数据库配置文件到模板成功: {} -> {}", sourcePath, targetPath);
            } catch (IOException e) {
                logger.error("复制数据库配置文件到模板失败: {} -> {}", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath(), e);
            }
        }
    }

    @Override
    public void deleteProject(String projectId) {
        // 检查项目是否正在执行
        if (executeService.isProjectExecuting(projectId)) {
            throw new ExecuteException("项目正在执行静态分析，请稍后再试");
        }

        String projectDir = configService.getProjectConfDir() + File.separator + projectId;
        if (!FileUtil.exists(projectDir)) {
            throw new ProjectNotFoundException("项目不存在: " + projectId);
        }

        // 删除项目目录
        if (!FileUtil.deleteDirectory(projectDir)) {
            throw new ConfigException("删除项目目录失败");
        }

        // 从项目列表中移除
        removeFromProjectList(projectId);

        logger.info("删除项目成功: projectId={}", projectId);
    }

    @Override
    public void deleteProjectByMcp(String projectId) {
        // 先检查项目是否存在
        if (!projectExists(projectId)) {
            throw new ConfigException("项目不存在: " + projectId);
        }

        // 检查项目是否通过MCP创建
        if (!isProjectCreatedByMcp(projectId)) {
            throw new ConfigException("仅允许删除通过MCP创建的项目: " + projectId);
        }

        // 通过MCP删除项目，调用已有的删除方法
        deleteProject(projectId);
    }

    @Override
    public Map<String, Object> batchDeleteProjects(List<String> projectIds) {
        int successCount = 0;
        int failCount = 0;
        List<Map<String, String>> errors = new ArrayList<>();

        for (String projectId : projectIds) {
            try {
                deleteProject(projectId);
                successCount++;
            } catch (Exception e) {
                failCount++;
                Map<String, String> error = new HashMap<>();
                error.put("projectId", projectId);
                error.put("error", e.getMessage());
                errors.add(error);
                logger.warn("批量删除项目失败: projectId={}, error={}", projectId, e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors);
        return result;
    }

    @Override
    public ProjectVO copyProject(String projectId, String description) {
        // 检查项目是否正在执行
        if (executeService.isProjectExecuting(projectId)) {
            throw new ExecuteException("项目正在执行静态分析，请稍后再试");
        }

        ProjectVO sourceProject = getProject(projectId);

        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setDescription(description != null ? description : sourceProject.getDescription() + "-副本");
        projectDTO.setJavacg2Config(sourceProject.getJavacg2Config());
        projectDTO.setJacgConfig(sourceProject.getJacgConfig());
        // 复制项目时不复制项目根目录（根目录全局唯一）

        return createProject(projectDTO);
    }

    /**
     * 更新项目列表
     */
    private void updateProjectList(String projectId, String description, String projectRootDir, String createTime, String updateTime, boolean isUpdate, boolean createdByMcp) {
        String projectConfDir = configService.getProjectConfDir();
        if (!FileUtil.createDirectory(projectConfDir)) {
            throw new ConfigException("创建项目配置目录失败");
        }

        String projectJsonPath = projectConfDir + File.separator + Constants.PROJECT_JSON_FILE;
        File projectJsonFile = new File(projectJsonPath);

        ProjectListEntity projectListEntity = new ProjectListEntity();

        if (projectJsonFile.exists()) {
            ProjectListEntity existingEntity = JsonUtil.fromFile(projectJsonFile, ProjectListEntity.class);
            if (existingEntity != null && existingEntity.getProjects() != null) {
                projectListEntity.setProjects(existingEntity.getProjects());
            }
        }

        List<ProjectListEntity.ProjectListItem> projects = projectListEntity.getProjects();

        if (isUpdate) {
            // 更新现有项目
            for (ProjectListEntity.ProjectListItem item : projects) {
                if (projectId.equals(item.getProjectId())) {
                    item.setDescription(description);
                    item.setProjectRootDir(projectRootDir);
                    item.setUpdateTime(updateTime);
                    break;
                }
            }
        } else {
            // 添加新项目
            ProjectListEntity.ProjectListItem newItem = new ProjectListEntity.ProjectListItem();
            newItem.setProjectId(projectId);
            newItem.setDescription(description);
            newItem.setProjectRootDir(projectRootDir);
            newItem.setCreateTime(createTime);
            newItem.setUpdateTime(updateTime);
            newItem.setCreatedByMcp(createdByMcp);
            projects.add(newItem);
        }

        if (!JsonUtil.toFile(projectJsonFile, projectListEntity)) {
            throw new ConfigException("保存项目列表失败");
        }
    }

    /**
     * 从项目列表中移除项目
     */
    private void removeFromProjectList(String projectId) {
        String projectJsonPath = configService.getProjectConfDir() + File.separator + Constants.PROJECT_JSON_FILE;
        File projectJsonFile = new File(projectJsonPath);

        if (!projectJsonFile.exists()) {
            return;
        }

        ProjectListEntity projectListEntity = JsonUtil.fromFile(projectJsonFile, ProjectListEntity.class);
        if (projectListEntity == null || projectListEntity.getProjects() == null) {
            return;
        }

        projectListEntity.getProjects().removeIf(item -> projectId.equals(item.getProjectId()));
        JsonUtil.toFile(projectJsonFile, projectListEntity);
    }

    /**
     * 检查项目描述是否已存在
     *
     * @param description 项目描述
     * @param excludeProjectId 排除的项目ID（更新时使用）
     * @return true-已存在，false-不存在
     */
    private boolean isProjectDescExists(String description, String excludeProjectId) {
        if (description == null || description.isEmpty()) {
            return false;
        }
        String projectJsonPath = configService.getProjectConfDir() + File.separator + Constants.PROJECT_JSON_FILE;
        File projectJsonFile = new File(projectJsonPath);

        if (!projectJsonFile.exists()) {
            return false;
        }

        ProjectListEntity projectListEntity = JsonUtil.fromFile(projectJsonFile, ProjectListEntity.class);
        if (projectListEntity == null || projectListEntity.getProjects() == null) {
            return false;
        }

        for (ProjectListEntity.ProjectListItem item : projectListEntity.getProjects()) {
            if (description.equals(item.getDescription())) {
                // 如果是更新操作，排除当前项目
                if (excludeProjectId != null && excludeProjectId.equals(item.getProjectId())) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 检查javacg2配置中是否有Jar路径配置
     *
     * @param javacg2Config javacg2配置
     * @return true-有配置，false-无配置
     */
    private boolean hasJarPaths(JavaCG2ConfigDTO javacg2Config) {
        if (javacg2Config == null || javacg2Config.getListConfig() == null) {
            return false;
        }
        List<String> jarPaths = javacg2Config.getListConfig().get(JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR.name());
        return jarPaths != null && !jarPaths.isEmpty();
    }

    /**
     * 规范化项目根目录路径
     * 使用 File.getCanonicalPath() 规范化路径，处理 "."、".."、符号链接等
     *
     * @param projectRootDir 原始项目根目录路径
     * @return 规范化后的路径，如果输入为空或空白则返回null
     */
    private String canonicalizeProjectRootDir(String projectRootDir) {
        if (projectRootDir == null || projectRootDir.trim().isEmpty()) {
            return null;
        }
        try {
            return new File(projectRootDir.trim()).getCanonicalPath();
        } catch (IOException e) {
            logger.error("规范化项目根目录失败: {}", projectRootDir, e);
            throw new ConfigException("项目根目录路径无效: " + projectRootDir);
        }
    }

    /**
     * 检查项目根目录是否已存在
     *
     * @param canonicalRootDir 规范化后的项目根目录
     * @param excludeProjectId 排除的项目ID（更新时使用）
     * @return true-已存在，false-不存在
     */
    private boolean isProjectRootDirExists(String canonicalRootDir, String excludeProjectId) {
        if (canonicalRootDir == null || canonicalRootDir.isEmpty()) {
            return false;
        }
        ProjectListEntity projectListEntity = loadProjectListEntity();
        if (projectListEntity == null || projectListEntity.getProjects() == null) {
            return false;
        }

        for (ProjectListEntity.ProjectListItem item : projectListEntity.getProjects()) {
            if (canonicalRootDir.equals(item.getProjectRootDir())) {
                if (excludeProjectId != null && excludeProjectId.equals(item.getProjectId())) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 根据项目根目录查找项目
     *
     * @param canonicalRootDir 规范化后的项目根目录
     * @return 匹配的项目列表项，未找到返回null
     */
    private ProjectListEntity.ProjectListItem findProjectByRootDir(String canonicalRootDir) {
        if (canonicalRootDir == null || canonicalRootDir.isEmpty()) {
            return null;
        }
        ProjectListEntity projectListEntity = loadProjectListEntity();
        if (projectListEntity == null || projectListEntity.getProjects() == null) {
            return null;
        }

        for (ProjectListEntity.ProjectListItem item : projectListEntity.getProjects()) {
            if (canonicalRootDir.equals(item.getProjectRootDir())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 加载项目列表实体
     */
    private ProjectListEntity loadProjectListEntity() {
        String projectJsonPath = configService.getProjectConfDir() + File.separator + Constants.PROJECT_JSON_FILE;
        File projectJsonFile = new File(projectJsonPath);
        if (!projectJsonFile.exists()) {
            return null;
        }
        return JsonUtil.fromFile(projectJsonFile, ProjectListEntity.class);
    }

    @Override
    public Map<String, Object> queryProjectByRootDir(String projectRootDir) {
        String canonicalRootDir = canonicalizeProjectRootDir(projectRootDir);
        Map<String, Object> result = new HashMap<>();
        result.put("found", false);

        if (canonicalRootDir == null) {
            return result;
        }

        ProjectListEntity.ProjectListItem item = findProjectByRootDir(canonicalRootDir);
        if (item != null) {
            result.put("found", true);
            result.put("projectId", item.getProjectId());
            result.put("description", item.getDescription());
            result.put("projectRootDir", item.getProjectRootDir());
            result.put("createTime", item.getCreateTime());
            result.put("updateTime", item.getUpdateTime());
        }

        return result;
    }

    @Override
    public boolean isProjectDescriptionExists(String description) {
        return isProjectDescExists(description, null);
    }

    @Override
    public boolean projectExists(String projectId) {
        String projectDir = configService.getProjectConfDir() + File.separator + projectId;
        return FileUtil.exists(projectDir);
    }

    /**
     * 判断项目是否通过MCP创建
     *
     * @param projectId 项目ID
     * @return true-通过MCP创建，false-通过HTTP创建
     */
    private boolean isProjectCreatedByMcp(String projectId) {
        ProjectListEntity projectListEntity = loadProjectListEntity();
        if (projectListEntity == null || projectListEntity.getProjects() == null) {
            return false;
        }

        for (ProjectListEntity.ProjectListItem item : projectListEntity.getProjects()) {
            if (projectId.equals(item.getProjectId())) {
                return item.isCreatedByMcp();
            }
        }
        return false;
    }
}
