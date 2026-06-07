package com.github.adrninistrator.jacgserver.controller;

import com.github.adrninistrator.jacgserver.model.dto.TemplateDTO;
import com.github.adrninistrator.jacgserver.model.vo.ResponseResult;
import com.github.adrninistrator.jacgserver.model.vo.TemplateVO;
import com.github.adrninistrator.jacgserver.service.TemplateService;
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
 * 模板管理控制器
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1")
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    /**
     * 获取模板列表
     */
    @GetMapping("/projects/{projectId}/templates")
    public ResponseResult listTemplates(@PathVariable String projectId) {
        List<TemplateVO> templates = templateService.listTemplates(projectId);
        Map<String, Object> data = new HashMap<>();
        data.put("templates", templates);
        return ResponseUtil.success(data);
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/projects/{projectId}/templates/{templateId}")
    public ResponseResult getTemplate(@PathVariable String projectId, @PathVariable String templateId) {
        TemplateVO template = templateService.getTemplate(projectId, templateId);
        return ResponseUtil.success(template);
    }

    /**
     * 创建模板
     */
    @PostMapping("/projects/{projectId}/templates")
    public ResponseResult createTemplate(@PathVariable String projectId, @RequestBody TemplateDTO templateDTO) {
        TemplateVO template = templateService.createTemplate(projectId, templateDTO);
        return ResponseUtil.success(template);
    }

    /**
     * 更新模板
     */
    @PutMapping("/projects/{projectId}/templates/{templateId}")
    public ResponseResult updateTemplate(@PathVariable String projectId, @PathVariable String templateId, @RequestBody TemplateDTO templateDTO) {
        templateService.updateTemplate(projectId, templateId, templateDTO);
        return ResponseUtil.success();
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/projects/{projectId}/templates/{templateId}")
    public ResponseResult deleteTemplate(@PathVariable String projectId, @PathVariable String templateId) {
        templateService.deleteTemplate(projectId, templateId);
        return ResponseUtil.success();
    }

    /**
     * 复制模板
     */
    @PostMapping("/projects/{projectId}/templates/{templateId}/copy")
    public ResponseResult copyTemplate(@PathVariable String projectId, @PathVariable String templateId, @RequestBody Map<String, String> request) {
        String description = request.get("description");
        TemplateVO template = templateService.copyTemplate(projectId, templateId, description);
        return ResponseUtil.success(template);
    }
}
