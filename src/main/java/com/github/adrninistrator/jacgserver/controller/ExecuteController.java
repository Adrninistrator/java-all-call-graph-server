package com.github.adrninistrator.jacgserver.controller;

import com.github.adrninistrator.jacgserver.model.vo.ExecutionVO;
import com.github.adrninistrator.jacgserver.model.vo.ResponseResult;
import com.github.adrninistrator.jacgserver.service.ExecuteService;
import com.github.adrninistrator.jacgserver.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行控制器
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1")
public class ExecuteController {

    @Autowired
    private ExecuteService executeService;

    /**
     * 执行静态分析
     */
    @PostMapping("/projects/{projectId}/execute/analysis")
    public ResponseResult executeAnalysis(@PathVariable String projectId) {
        ExecutionVO execution = executeService.executeAnalysis(projectId);
        return ResponseUtil.success(execution);
    }

    /**
     * 执行调用链生成
     */
    @PostMapping("/templates/{templateId}/execute/callgraph")
    public ResponseResult executeCallGraph(@PathVariable String templateId) {
        ExecutionVO execution = executeService.executeCallGraph(templateId);
        return ResponseUtil.success(execution);
    }

    /**
     * 执行根据关键字生成调用堆栈
     */
    @PostMapping("/templates/{templateId}/execute/findstack")
    public ResponseResult executeFindStack(@PathVariable String templateId) {
        ExecutionVO execution = executeService.executeFindStack(templateId);
        return ResponseUtil.success(execution);
    }

    /**
     * 查询执行状态
     */
    @GetMapping("/executions/{execId}/status")
    public ResponseResult getExecutionStatus(@PathVariable String execId) {
        ExecutionVO execution = executeService.getExecutionStatus(execId);
        return ResponseUtil.success(execution);
    }

    /**
     * 检查项目是否正在执行静态分析
     */
    @GetMapping("/projects/{projectId}/executing")
    public ResponseResult isProjectExecuting(@PathVariable String projectId) {
        Map<String, Object> data = new HashMap<>();
        data.put("executing", executeService.isProjectExecuting(projectId));
        // 返回执行状态信息（包含执行耗时）
        ExecutionVO executionInfo = executeService.getProjectExecutionInfo(projectId);
        if (executionInfo != null) {
            data.put("duration", executionInfo.getDuration());
            data.put("status", executionInfo.getStatus());
        }
        return ResponseUtil.success(data);
    }

    /**
     * 检查模板是否正在执行调用链生成
     */
    @GetMapping("/templates/{templateId}/executing")
    public ResponseResult isTemplateExecuting(@PathVariable String templateId) {
        Map<String, Object> data = new HashMap<>();
        data.put("executing", executeService.isTemplateExecuting(templateId));
        // 返回执行状态信息（包含执行耗时）
        ExecutionVO executionInfo = executeService.getTemplateExecutionInfo(templateId);
        if (executionInfo != null) {
            data.put("duration", executionInfo.getDuration());
            data.put("status", executionInfo.getStatus());
        }
        return ResponseUtil.success(data);
    }

    /**
     * 获取执行日志
     */
    @GetMapping("/executions/{execId}/logs")
    public ResponseResult getExecutionLogs(
            @PathVariable String execId,
            @RequestParam(required = false, defaultValue = "100") int lines) {
        List<String> logs = executeService.getExecutionLogs(execId, lines);
        Map<String, Object> data = new HashMap<>();
        data.put("logs", logs);
        return ResponseUtil.success(data);
    }
}
