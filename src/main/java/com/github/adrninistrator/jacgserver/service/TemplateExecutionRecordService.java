package com.github.adrninistrator.jacgserver.service;

import com.github.adrninistrator.jacgserver.model.entity.CallGraphExecutionRecord;
import com.github.adrninistrator.jacgserver.model.entity.CallGraphFileInfo;
import com.github.adrninistrator.jacgserver.model.entity.FindStackExecutionRecord;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 模板执行记录服务接口
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public interface TemplateExecutionRecordService {

    // ==================== 调用链记录 ====================

    /**
     * 保存调用链执行记录
     */
    CallGraphExecutionRecord saveCallGraphRecord(CallGraphExecutionRecord record);

    /**
     * 更新调用链执行状态
     */
    void updateCallGraphStatus(Long id, String status, Long duration, String outputDir, String logFilePath, String errorMessage);

    /**
     * 根据执行ID查询调用链执行记录
     */
    CallGraphExecutionRecord getCallGraphRecordByExecId(String execId);

    /**
     * 查询调用链执行记录列表（分页）
     * @param templateId 模板ID
     * @param minStartTime 最小开始时间（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 包含records和total的Map
     */
    Map<String, Object> queryCallGraphRecords(String templateId, Date minStartTime, int pageNum, int pageSize);

    /**
     * 根据ID查询调用链执行记录
     */
    CallGraphExecutionRecord getCallGraphRecordById(Long id);

    // ==================== 调用链文件信息 ====================

    /**
     * 批量保存调用链文件信息
     * @param recordId 调用链执行记录ID
     * @param fileInfoList 调用链文件信息列表
     */
    void saveCallGraphFileInfoList(Long recordId, List<CallGraphFileInfo> fileInfoList);

    /**
     * 根据执行记录ID查询调用链文件信息列表
     * @param recordId 调用链执行记录ID
     * @return 调用链文件信息列表
     */
    List<CallGraphFileInfo> getCallGraphFileInfoByRecordId(Long recordId);

    /**
     * 根据执行ID（execId）查询调用链文件信息列表
     * @param execId 执行ID
     * @return 调用链文件信息列表
     */
    List<CallGraphFileInfo> getCallGraphFileInfoByExecId(String execId);

    // ==================== 关键字生成堆栈记录 ====================

    /**
     * 保存关键字生成堆栈执行记录
     */
    FindStackExecutionRecord saveFindStackRecord(FindStackExecutionRecord record);

    /**
     * 更新关键字生成堆栈执行状态
     */
    void updateFindStackStatus(Long id, String status, Long duration, String outputDir, String errorMessage);

    /**
     * 查询关键字生成堆栈执行记录列表（分页）
     * @param templateId 模板ID
     * @param minStartTime 最小开始时间（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 包含records和total的Map
     */
    Map<String, Object> queryFindStackRecords(String templateId, Date minStartTime, int pageNum, int pageSize);

    /**
     * 根据ID查询关键字生成堆栈执行记录
     */
    FindStackExecutionRecord getFindStackRecordById(Long id);
}