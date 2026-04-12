package com.github.adrninistrator.jacgserver.service;

import com.github.adrninistrator.jacgserver.model.dto.ExecutionRecordQueryDTO;
import com.github.adrninistrator.jacgserver.model.entity.AnalysisExecutionRecord;
import com.github.adrninistrator.jacgserver.model.vo.ExecutionRecordVO;

/**
 * 执行记录服务接口
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public interface ExecutionRecordService {

    /**
     * 保存执行记录
     *
     * @param record 执行记录
     * @return 保存后的记录（包含ID）
     */
    AnalysisExecutionRecord saveRecord(AnalysisExecutionRecord record);

    /**
     * 更新执行状态
     *
     * @param id           主键ID
     * @param status       执行状态
     * @param duration     执行耗时（毫秒）
     * @param errorMessage 错误信息（可为null）
     */
    void updateStatus(Long id, String status, Long duration, String errorMessage);

    /**
     * 查询执行记录（分页）
     *
     * @param queryDTO 查询参数
     * @return 执行记录列表（带分页信息）
     */
    ExecutionRecordVO queryRecords(ExecutionRecordQueryDTO queryDTO);

    /**
     * 根据执行ID查询执行记录
     *
     * @param execId 执行ID
     * @return 执行记录
     */
    AnalysisExecutionRecord getByExecId(String execId);

    /**
     * 根据主键ID查询执行记录详情
     *
     * @param id 主键ID
     * @return 执行记录
     */
    AnalysisExecutionRecord getById(Long id);

    /**
     * 查询项目最近一条成功的执行记录
     *
     * @param projectId 项目ID
     * @return 最近一条成功的执行记录，不存在则返回null
     */
    AnalysisExecutionRecord getLatestSuccessByProjectId(String projectId);
}
