package com.github.adrninistrator.jacgserver.service;

import com.github.adrninistrator.jacgserver.model.dto.TemplateDTO;
import com.github.adrninistrator.jacgserver.model.vo.TemplateVO;

import java.util.List;

/**
 * 模板管理服务接口
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public interface TemplateService {

    /**
     * 获取模板列表
     *
     * @param projectId 项目ID
     * @return 模板列表
     */
    List<TemplateVO> listTemplates(String projectId);

    /**
     * 获取模板详情
     *
     * @param templateId 模板ID
     * @return 模板详情
     */
    TemplateVO getTemplate(String templateId);

    /**
     * 创建模板
     *
     * @param projectId   项目ID
     * @param templateDTO 模板信息
     * @return 创建的模板
     */
    TemplateVO createTemplate(String projectId, TemplateDTO templateDTO);

    /**
     * 更新模板
     *
     * @param templateId  模板ID
     * @param templateDTO 模板信息
     */
    void updateTemplate(String templateId, TemplateDTO templateDTO);

    /**
     * 删除模板
     *
     * @param templateId 模板ID
     */
    void deleteTemplate(String templateId);

    /**
     * 复制模板
     *
     * @param templateId  源模板ID
     * @param description 新模板描述
     * @return 新模板
     */
    TemplateVO copyTemplate(String templateId, String description);
}
