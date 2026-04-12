package com.github.adrninistrator.jacgserver.mapper;

import com.github.adrninistrator.jacgserver.model.entity.CallGraphExecutionRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 模板生成调用链记录Mapper
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Mapper
public interface CallGraphExecutionRecordMapper {

    /**
     * 插入记录
     */
    @Insert("INSERT INTO call_graph_execution_record (exec_id, project_id, template_id, direction, entry_methods, " +
            "start_time, status, output_dir) VALUES (#{execId}, #{projectId}, #{templateId}, #{direction}, " +
            "#{entryMethods}, #{startTime}, #{status}, #{outputDir})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CallGraphExecutionRecord record);

    /**
     * 更新状态
     */
    @Update("UPDATE call_graph_execution_record SET status = #{status}, end_time = #{endTime}, " +
            "duration = #{duration}, output_dir = #{outputDir}, error_message = #{errorMessage} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("endTime") java.util.Date endTime,
                     @Param("duration") Long duration, @Param("outputDir") String outputDir, @Param("errorMessage") String errorMessage);

    /**
     * 根据模板ID查询记录列表
     */
    @Select("SELECT * FROM call_graph_execution_record WHERE template_id = #{templateId} ORDER BY start_time DESC")
    List<CallGraphExecutionRecord> findByTemplateId(@Param("templateId") String templateId);

    /**
     * 根据项目ID查询记录列表
     */
    @Select("SELECT * FROM call_graph_execution_record WHERE project_id = #{projectId} ORDER BY start_time DESC")
    List<CallGraphExecutionRecord> findByProjectId(@Param("projectId") String projectId);

    /**
     * 分页查询
     */
    @Select("SELECT * FROM call_graph_execution_record WHERE template_id = #{templateId} " +
            "AND start_time >= #{minStartTime} ORDER BY start_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<CallGraphExecutionRecord> findByTemplateIdWithPage(@Param("templateId") String templateId,
            @Param("minStartTime") java.util.Date minStartTime, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 统计数量
     */
    @Select("SELECT COUNT(*) FROM call_graph_execution_record WHERE template_id = #{templateId} AND start_time >= #{minStartTime}")
    int countByTemplateId(@Param("templateId") String templateId, @Param("minStartTime") java.util.Date minStartTime);

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM call_graph_execution_record WHERE id = #{id}")
    CallGraphExecutionRecord findById(@Param("id") Long id);
}