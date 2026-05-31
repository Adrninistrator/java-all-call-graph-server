package com.github.adrninistrator.jacgserver.controller;

import com.github.adrninistrator.jacgserver.model.dto.ProjectDTO;
import com.github.adrninistrator.jacgserver.model.vo.ProjectVO;
import com.github.adrninistrator.jacgserver.model.vo.ResponseResult;
import com.github.adrninistrator.jacgserver.service.ProjectService;
import com.github.adrninistrator.jacgserver.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目管理控制器
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    /**
     * 获取项目列表
     */
    @GetMapping
    public ResponseResult listProjects() {
        List<ProjectVO> projects = projectService.listProjects();
        Map<String, Object> data = new HashMap<>();
        data.put("projects", projects);
        return ResponseUtil.success(data);
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/{projectId}")
    public ResponseResult getProject(@PathVariable String projectId) {
        ProjectVO project = projectService.getProject(projectId);
        return ResponseUtil.success(project);
    }

    /**
     * 创建项目
     */
    @PostMapping
    public ResponseResult createProject(@RequestBody ProjectDTO projectDTO) {
        ProjectVO project = projectService.createProject(projectDTO);
        return ResponseUtil.success(project);
    }

    /**
     * 更新项目
     */
    @PutMapping("/{projectId}")
    public ResponseResult updateProject(@PathVariable String projectId, @RequestBody ProjectDTO projectDTO) {
        projectService.updateProject(projectId, projectDTO);
        return ResponseUtil.success();
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/{projectId}")
    public ResponseResult deleteProject(@PathVariable String projectId) {
        projectService.deleteProject(projectId);
        return ResponseUtil.success();
    }

    /**
     * 批量删除项目
     */
    @PostMapping("/batch-delete")
    public ResponseResult batchDeleteProjects(@RequestBody Map<String, List<String>> request) {
        List<String> projectIds = request.get("projectIds");
        if (projectIds == null || projectIds.isEmpty()) {
            return ResponseUtil.error(400, "请选择要删除的项目");
        }
        Map<String, Object> result = projectService.batchDeleteProjects(projectIds);
        return ResponseUtil.success(result);
    }

    /**
     * 复制项目
     */
    @PostMapping("/{projectId}/copy")
    public ResponseResult copyProject(@PathVariable String projectId, @RequestBody Map<String, String> request) {
        String description = request.get("description");
        ProjectVO project = projectService.copyProject(projectId, description);
        return ResponseUtil.success(project);
    }

}
