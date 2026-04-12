package com.github.adrninistrator.jacgserver.mapper;

import com.github.adrninistrator.jacgserver.model.entity.FindStackExecutionRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 模板生成调用链根据关键字生成堆栈记录Mapper
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Mapper
public interface FindStackExecutionRecordMapper {

    /**
     * 插入记录
     */
    @Insert("INSERT INTO find_stack_execution_record (exec_id, project_id, template_id, direction, entry_methods, " +
            "keywords, start_time, status, output_dir) VALUES (#{execId}, #{projectId}, #{templateId}, #{direction}, " +
            "#{entryMethods}, #{keywords}, #{startTime}, #{status}, #{outputDir})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FindStackExecutionRecord record);

    /**
     * 更新状态
     */
    @Update("UPDATE find_stack_execution_record SET status = #{status}, end_time = #{endTime}, " +
            "duration = #{duration}, output_dir = #{outputDir}, error_message = #{errorMessage} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("endTime") java.util.Date endTime,
                     @Param("duration") Long duration, @Param("outputDir") String outputDir, @Param("errorMessage") String errorMessage);

    /**
     * 根据模板ID查询记录列表
     */
    @Select("SELECT * FROM find_stack_execution_record WHERE template_id = #{templateId} ORDER BY start_time DESC")
    List<FindStackExecutionRecord> findByTemplateId(@Param("templateId") String templateId);

    /**
     * 根据项目ID查询记录列表
     */
    @Select("SELECT * FROM find_stack_execution_record WHERE project_id = #{projectId} ORDER BY start_time DESC")
    List<FindStackExecutionRecord> findByProjectId(@Param("projectId") String projectId);

    /**
     * 分页查询
     */
    @Select("SELECT * FROM find_stack_execution_record WHERE template_id = #{templateId} " +
            "AND start_time >= #{minStartTime} ORDER BY start_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<FindStackExecutionRecord> findByTemplateIdWithPage(@Param("templateId") String templateId,
            @Param("minStartTime") java.util.Date minStartTime, @Param("limit") int limit, @Param("offset") int offset);

    /**
     * 统计数量
     */
    @Select("SELECT COUNT(*) FROM find_stack_execution_record WHERE template_id = #{templateId} AND start_time >= #{minStartTime}")
    int countByTemplateId(@Param("templateId") String templateId, @Param("minStartTime") java.util.Date minStartTime);

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM find_stack_execution_record WHERE id = #{id}")
    FindStackExecutionRecord findById(@Param("id") Long id);
}