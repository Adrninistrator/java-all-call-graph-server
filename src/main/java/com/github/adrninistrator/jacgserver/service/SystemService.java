package com.github.adrninistrator.jacgserver.service;

import com.github.adrninistrator.jacgserver.service.impl.SystemServiceImpl;

/**
 * 系统服务接口
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public interface SystemService {

    /**
     * 打开目录（仅支持Windows）
     *
     * @param directoryPath 目录路径
     * @return 打开结果，包含是否成功和消息
     */
    SystemServiceImpl.OpenDirectoryResult openDirectory(String directoryPath);

    /**
     * 获取应用根目录
     *
     * @return 应用根目录路径
     */
    String getAppRootPath();

    /**
     * 打开项目日志目录
     *
     * @param projectId 项目ID
     * @return 打开结果，包含是否成功和消息
     */
    SystemServiceImpl.OpenDirectoryResult openProjectLogDirectory(String projectId);

    /**
     * 打开执行记录的输出目录（安全方式）
     * 后端根据记录类型和ID从数据库查询路径
     *
     * @param recordType 记录类型：callgraph（调用链）、findstack（关键字生成堆栈）
     * @param recordId   记录ID
     * @return 打开结果，包含是否成功和消息
     */
    SystemServiceImpl.OpenDirectoryResult openExecutionOutputDirectory(String recordType, Long recordId);
}
