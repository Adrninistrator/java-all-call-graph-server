package com.github.adrninistrator.jacgserver.mapper;

import com.github.adrninistrator.jacgserver.model.entity.AnalysisExecutionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 项目静态解析执行记录 Mapper 接口
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Mapper
public interface AnalysisExecutionRecordMapper {

    /**
     * 插入执行记录
     *
     * @param record 执行记录
     * @return 影响行数
     */
    int insert(AnalysisExecutionRecord record);

    /**
     * 根据ID查询执行记录
     *
     * @param id 主键ID
     * @return 执行记录
     */
    AnalysisExecutionRecord selectById(Long id);

    /**
     * 根据执行ID查询执行记录
     *
     * @param execId 执行ID
     * @return 执行记录
     */
    AnalysisExecutionRecord selectByExecId(String execId);

    /**
     * 根据项目ID查询执行记录列表（分页）
     *
     * @param projectId     项目ID
     * @param minStartTime  最小开始时间
     * @param offset        偏移量
     * @param pageSize      每页数量
     * @return 执行记录列表
     */
    List<AnalysisExecutionRecord> selectByProjectIdWithPage(
            @Param("projectId") String projectId,
            @Param("minStartTime") Date minStartTime,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    /**
     * 统计项目执行记录数量
     *
     * @param projectId     项目ID
     * @param minStartTime  最小开始时间
     * @return 记录数量
     */
    int countByProjectId(
            @Param("projectId") String projectId,
            @Param("minStartTime") Date minStartTime);

    /**
     * 更新执行状态
     *
     * @param id           主键ID
     * @param status       执行状态
     * @param endTime      结束时间
     * @param duration     执行耗时（毫秒）
     * @param errorMessage 错误信息
     * @return 影响行数
     */
    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("endTime") Date endTime,
            @Param("duration") Long duration,
            @Param("errorMessage") String errorMessage,
            @Param("logFilePath") String logFilePath);

    /**
     * 查询项目最近一条成功的执行记录
     *
     * @param projectId 项目ID
     * @return 最近一条成功的执行记录，不存在则返回null
     */
    AnalysisExecutionRecord selectLatestSuccessByProjectId(@Param("projectId") String projectId);
}
