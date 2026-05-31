package com.github.adrninistrator.jacgserver.service;

import com.github.adrninistrator.jacgserver.model.dto.ProjectDTO;
import com.github.adrninistrator.jacgserver.model.vo.ProjectVO;

import java.util.List;
import java.util.Map;

/**
 * 项目管理服务接口
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public interface ProjectService {

    /**
     * 获取项目列表
     *
     * @return 项目列表
     */
    List<ProjectVO> listProjects();

    /**
     * 获取项目详情
     *
     * @param projectId 项目ID
     * @return 项目详情
     */
    ProjectVO getProject(String projectId);

    /**
     * 创建项目
     *
     * @param projectDTO 项目信息
     * @return 创建的项目
     */
    ProjectVO createProject(ProjectDTO projectDTO);

    /**
     * 更新项目
     *
     * @param projectId  项目ID
     * @param projectDTO 项目信息
     */
    void updateProject(String projectId, ProjectDTO projectDTO);

    /**
     * 删除项目
     *
     * @param projectId 项目ID
     */
    void deleteProject(String projectId);

    /**
     * 通过MCP删除项目（仅允许删除通过MCP创建的项目）
     *
     * @param projectId 项目ID
     */
    void deleteProjectByMcp(String projectId);

    /**
     * 批量删除项目
     *
     * @param projectIds 项目ID列表
     * @return 包含成功删除数、失败数和详细错误信息的Map
     */
    Map<String, Object> batchDeleteProjects(List<String> projectIds);

    /**
     * 复制项目
     *
     * @param projectId   源项目ID
     * @param description 新项目描述
     * @return 新项目
     */
    ProjectVO copyProject(String projectId, String description);

    /**
     * 根据项目根目录查询项目ID
     * 对请求参数中的项目根目录使用 File.getCanonicalPath() 规范化
     *
     * @param projectRootDir 项目根目录
     * @return 查询结果，包含 found（是否找到）、projectId、description 等字段
     */
    Map<String, Object> queryProjectByRootDir(String projectRootDir);

    /**
     * 判断项目描述是否已存在
     *
     * @param description 项目描述
     * @return true-已存在，false-不存在
     */
    boolean isProjectDescriptionExists(String description);

    /**
     * 判断项目是否存在
     *
     * @param projectId 项目ID
     * @return true-存在，false-不存在
     */
    boolean projectExists(String projectId);
}
