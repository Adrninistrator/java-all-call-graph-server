package com.github.adrninistrator.jacgserver.service;

import com.github.adrninistrator.jacgserver.model.vo.ExecutionVO;

import java.util.List;

/**
 * 执行服务接口
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public interface ExecuteService {

    /**
     * 执行静态分析
     *
     * @param projectId 项目ID
     * @return 执行信息
     */
    ExecutionVO executeAnalysis(String projectId);

    /**
     * 执行调用链生成
     *
     * @param templateId 模板ID
     * @return 执行信息
     */
    ExecutionVO executeCallGraph(String templateId);

    /**
     * 查询执行状态
     *
     * @param execId 执行ID
     * @return 执行信息
     */
    ExecutionVO getExecutionStatus(String execId);

    /**
     * 获取执行日志
     *
     * @param execId 执行ID
     * @param lines  行数
     * @return 日志内容
     */
    List<String> getExecutionLogs(String execId, int lines);

    /**
     * 检查项目是否正在执行静态分析
     *
     * @param projectId 项目ID
     * @return 是否正在执行
     */
    boolean isProjectExecuting(String projectId);

    /**
     * 检查模板是否正在执行调用链生成
     *
     * @param templateId 模板ID
     * @return 是否正在执行
     */
    boolean isTemplateExecuting(String templateId);

    /**
     * 获取项目当前执行状态信息
     *
     * @param projectId 项目ID
     * @return 执行信息（包含执行状态和耗时）
     */
    ExecutionVO getProjectExecutionInfo(String projectId);

    /**
     * 获取模板当前执行状态信息
     *
     * @param templateId 模板ID
     * @return 执行信息（包含执行状态和耗时）
     */
    ExecutionVO getTemplateExecutionInfo(String templateId);

    /**
     * 执行根据关键字生成调用堆栈
     *
     * @param templateId 模板ID
     * @return 执行信息
     */
    ExecutionVO executeFindStack(String templateId);
}
