package com.github.adrninistrator.jacgserver.controller;

import com.github.adrninistrator.jacgserver.model.vo.ResponseResult;
import com.github.adrninistrator.jacgserver.service.ConfigService;
import com.github.adrninistrator.jacgserver.service.SystemService;
import com.github.adrninistrator.jacgserver.service.impl.SystemServiceImpl;
import com.github.adrninistrator.jacgserver.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统控制器
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private SystemService systemService;

    /**
     * 获取输出根目录
     */
    @GetMapping("/output-root-path")
    public ResponseResult getOutputRootPath() {
        Map<String, Object> data = new HashMap<>();
        data.put("outputRootPath", configService.getOutputRootPath());
        return ResponseUtil.success(data);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseResult health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        return ResponseUtil.success(data);
    }

    /**
     * 获取应用根目录
     */
    @GetMapping("/app-root-path")
    public ResponseResult getAppRootPath() {
        Map<String, Object> data = new HashMap<>();
        data.put("appRootPath", systemService.getAppRootPath());
        return ResponseUtil.success(data);
    }

    /**
     * 打开应用根目录
     */
    @PostMapping("/open-app-directory")
    public ResponseResult openAppDirectory() {
        String appRootPath = systemService.getAppRootPath();
        SystemServiceImpl.OpenDirectoryResult result = systemService.openDirectory(appRootPath);
        if (result.isSuccess()) {
            return ResponseUtil.success(result.getMessage());
        } else {
            return ResponseUtil.error(500, result.getMessage());
        }
    }

    /**
     * 打开项目日志目录
     *
     * @param request 请求体，包含projectId字段
     * @return 操作结果
     */
    @PostMapping("/open-project-log-directory")
    public ResponseResult openProjectLogDirectory(@RequestBody Map<String, String> request) {
        String projectId = request.get("projectId");
        if (projectId == null || projectId.trim().isEmpty()) {
            return ResponseUtil.error(400, "项目ID不能为空");
        }

        SystemServiceImpl.OpenDirectoryResult result = systemService.openProjectLogDirectory(projectId);
        if (result.isSuccess()) {
            return ResponseUtil.success(result.getMessage());
        } else {
            return ResponseUtil.error(500, result.getMessage());
        }
    }

    /**
     * 打开执行记录的输出目录（安全方式）
     * 后端根据记录类型和ID从数据库查询路径，避免前端直接传递路径
     *
     * @param request 请求体，包含recordType和recordId字段
     * @return 操作结果
     */
    @PostMapping("/open-execution-output-directory")
    public ResponseResult openExecutionOutputDirectory(@RequestBody Map<String, Object> request) {
        String recordType = (String) request.get("recordType");
        Object recordIdObj = request.get("recordId");

        if (recordType == null || recordType.trim().isEmpty()) {
            return ResponseUtil.error(400, "记录类型不能为空");
        }
        if (recordIdObj == null) {
            return ResponseUtil.error(400, "记录ID不能为空");
        }

        Long recordId;
        try {
            if (recordIdObj instanceof Number) {
                recordId = ((Number) recordIdObj).longValue();
            } else {
                recordId = Long.parseLong(recordIdObj.toString());
            }
        } catch (NumberFormatException e) {
            return ResponseUtil.error(400, "记录ID格式不正确");
        }

        SystemServiceImpl.OpenDirectoryResult result = systemService.openExecutionOutputDirectory(recordType, recordId);
        if (result.isSuccess()) {
            return ResponseUtil.success(result.getMessage());
        } else {
            return ResponseUtil.error(500, result.getMessage());
        }
    }
}
