package com.github.adrninistrator.jacgserver.controller;

import com.github.adrninistrator.jacgserver.model.entity.CallGraphExecutionRecord;
import com.github.adrninistrator.jacgserver.model.entity.CallGraphFileInfo;
import com.github.adrninistrator.jacgserver.model.entity.FindStackExecutionRecord;
import com.github.adrninistrator.jacgserver.model.vo.ResponseResult;
import com.github.adrninistrator.jacgserver.service.TemplateExecutionRecordService;
import com.github.adrninistrator.jacgserver.util.ResponseUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 模板执行记录控制器
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/template/execution")
public class TemplateExecutionRecordController {

    @Resource
    private TemplateExecutionRecordService templateExecutionRecordService;

    /**
     * 查询调用链执行记录列表（分页）
     */
    @GetMapping("/call-graph/records/{templateId}")
    public ResponseResult queryCallGraphRecords(
            @PathVariable String templateId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date minStartTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> result = templateExecutionRecordService.queryCallGraphRecords(templateId, minStartTime, page, pageSize);
        return ResponseUtil.success(result);
    }

    /**
     * 查询调用链执行记录详情
     */
    @GetMapping("/call-graph/detail/{id}")
    public ResponseResult getCallGraphDetail(@PathVariable Long id) {
        CallGraphExecutionRecord record = templateExecutionRecordService.getCallGraphRecordById(id);
        if (record == null) {
            return ResponseUtil.error(404, "记录不存在");
        }
        return ResponseUtil.success(record);
    }

    /**
     * 查询调用链文件信息
     * 根据调用链执行记录ID查询生成的调用链文件路径信息
     */
    @GetMapping("/call-graph/files/{id}")
    public ResponseResult getCallGraphFiles(@PathVariable Long id) {
        List<CallGraphFileInfo> fileInfoList = templateExecutionRecordService.getCallGraphFileInfoByRecordId(id);
        return ResponseUtil.success(fileInfoList);
    }

    /**
     * 查询关键字生成堆栈执行记录列表（分页）
     */
    @GetMapping("/find-stack/records/{templateId}")
    public ResponseResult queryFindStackRecords(
            @PathVariable String templateId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date minStartTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> result = templateExecutionRecordService.queryFindStackRecords(templateId, minStartTime, page, pageSize);
        return ResponseUtil.success(result);
    }

    /**
     * 查询关键字生成堆栈执行记录详情
     */
    @GetMapping("/find-stack/detail/{id}")
    public ResponseResult getFindStackDetail(@PathVariable Long id) {
        FindStackExecutionRecord record = templateExecutionRecordService.getFindStackRecordById(id);
        if (record == null) {
            return ResponseUtil.error(404, "记录不存在");
        }
        return ResponseUtil.success(record);
    }
}