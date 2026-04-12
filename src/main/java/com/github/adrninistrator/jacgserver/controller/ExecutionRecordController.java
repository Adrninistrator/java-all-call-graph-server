package com.github.adrninistrator.jacgserver.controller;

import com.github.adrninistrator.jacgserver.model.dto.ExecutionRecordQueryDTO;
import com.github.adrninistrator.jacgserver.model.entity.AnalysisExecutionRecord;
import com.github.adrninistrator.jacgserver.model.vo.ExecutionRecordVO;
import com.github.adrninistrator.jacgserver.model.vo.ResponseResult;
import com.github.adrninistrator.jacgserver.service.ExecutionRecordService;
import com.github.adrninistrator.jacgserver.util.ResponseUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 执行记录 Controller
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/execution-records")
public class ExecutionRecordController {

    @Resource
    private ExecutionRecordService executionRecordService;

    /**
     * 查询项目执行记录（分页）
     *
     * @param projectId    项目ID
     * @param minStartTime 最小开始时间（可选）
     * @param pageNum      页码（从1开始）
     * @param pageSize     每页数量
     * @return 执行记录列表
     */
    @GetMapping("/project/{projectId}")
    public ResponseResult queryRecords(
            @PathVariable String projectId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date minStartTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        ExecutionRecordQueryDTO queryDTO = new ExecutionRecordQueryDTO();
        queryDTO.setProjectId(projectId);
        queryDTO.setMinStartTime(minStartTime);
        queryDTO.setPageNum(pageNum);
        queryDTO.setPageSize(pageSize);

        ExecutionRecordVO result = executionRecordService.queryRecords(queryDTO);
        return ResponseUtil.success(result);
    }

    /**
     * 获取执行记录详情
     *
     * @param id 记录ID
     * @return 执行记录详情
     */
    @GetMapping("/{id}")
    public ResponseResult getDetail(@PathVariable Long id) {
        AnalysisExecutionRecord record = executionRecordService.getById(id);
        if (record == null) {
            return ResponseUtil.error(404, "执行记录不存在");
        }
        return ResponseUtil.success(record);
    }
}
