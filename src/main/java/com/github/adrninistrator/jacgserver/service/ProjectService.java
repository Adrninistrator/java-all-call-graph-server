package com.github.adrninistrator.jacgserver.service;

import com.github.adrninistrator.jacgserver.model.dto.ProjectDTO;
import com.github.adrninistrator.jacgserver.model.vo.ProjectVO;

import java.util.List;

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
     * 复制项目
     *
     * @param projectId   源项目ID
     * @param description 新项目描述
     * @return 新项目
     */
    ProjectVO copyProject(String projectId, String description);
}
