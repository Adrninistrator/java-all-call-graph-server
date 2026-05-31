package com.github.adrninistrator.jacgserver.mapper;

import com.github.adrninistrator.jacgserver.model.entity.CallGraphFileInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 调用链文件信息Mapper
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Mapper
public interface CallGraphFileInfoMapper {

    /**
     * 批量插入调用链文件信息
     */
    @Insert("INSERT INTO call_graph_file_info (record_id, entry_method, orig_text, file_path) " +
            "VALUES (#{recordId}, #{entryMethod}, #{origText}, #{filePath})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CallGraphFileInfo fileInfo);

    /**
     * 根据执行记录ID查询调用链文件信息列表
     */
    @Select("SELECT * FROM call_graph_file_info WHERE record_id = #{recordId} ORDER BY id")
    List<CallGraphFileInfo> findByRecordId(@Param("recordId") Long recordId);

    /**
     * 根据执行记录ID删除调用链文件信息
     */
    @Delete("DELETE FROM call_graph_file_info WHERE record_id = #{recordId}")
    int deleteByRecordId(@Param("recordId") Long recordId);
}
