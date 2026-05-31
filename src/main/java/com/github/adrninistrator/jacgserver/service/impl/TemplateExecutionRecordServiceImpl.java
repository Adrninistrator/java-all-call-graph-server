package com.github.adrninistrator.jacgserver.service.impl;

import com.github.adrninistrator.jacgserver.mapper.CallGraphExecutionRecordMapper;
import com.github.adrninistrator.jacgserver.mapper.CallGraphFileInfoMapper;
import com.github.adrninistrator.jacgserver.mapper.FindStackExecutionRecordMapper;
import com.github.adrninistrator.jacgserver.model.entity.CallGraphExecutionRecord;
import com.github.adrninistrator.jacgserver.model.entity.CallGraphFileInfo;
import com.github.adrninistrator.jacgserver.model.entity.FindStackExecutionRecord;
import com.github.adrninistrator.jacgserver.service.TemplateExecutionRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板执行记录服务实现类
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Service
public class TemplateExecutionRecordServiceImpl implements TemplateExecutionRecordService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateExecutionRecordServiceImpl.class);

    @Resource
    private CallGraphExecutionRecordMapper callGraphExecutionRecordMapper;

    @Resource
    private CallGraphFileInfoMapper callGraphFileInfoMapper;

    @Resource
    private FindStackExecutionRecordMapper findStackExecutionRecordMapper;

    @Override
    public CallGraphExecutionRecord saveCallGraphRecord(CallGraphExecutionRecord record) {
        callGraphExecutionRecordMapper.insert(record);
        logger.info("保存调用链执行记录: id={}, execId={}", record.getId(), record.getExecId());
        return record;
    }

    @Override
    public void updateCallGraphStatus(Long id, String status, Long duration, String outputDir, String logFilePath, String errorMessage) {
        callGraphExecutionRecordMapper.updateStatus(id, status, new Date(), duration, outputDir, logFilePath, errorMessage);
        logger.info("更新调用链执行状态: id={}, status={}, outputDir={}, logFilePath={}", id, status, outputDir, logFilePath);
    }

    @Override
    public CallGraphExecutionRecord getCallGraphRecordByExecId(String execId) {
        return callGraphExecutionRecordMapper.findByExecId(execId);
    }

    @Override
    public Map<String, Object> queryCallGraphRecords(String templateId, Date minStartTime, int pageNum, int pageSize) {
        if (minStartTime == null) {
            minStartTime = new Date(0);
        }

        int total = callGraphExecutionRecordMapper.countByTemplateId(templateId, minStartTime);
        int offset = (pageNum - 1) * pageSize;
        List<CallGraphExecutionRecord> records = callGraphExecutionRecordMapper.findByTemplateIdWithPage(
                templateId, minStartTime, pageSize, offset);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    @Override
    public CallGraphExecutionRecord getCallGraphRecordById(Long id) {
        return callGraphExecutionRecordMapper.findById(id);
    }

    @Override
    public void saveCallGraphFileInfoList(Long recordId, List<CallGraphFileInfo> fileInfoList) {
        for (CallGraphFileInfo fileInfo : fileInfoList) {
            fileInfo.setRecordId(recordId);
            callGraphFileInfoMapper.insert(fileInfo);
        }
        logger.info("保存调用链文件信息: recordId={}, count={}", recordId, fileInfoList.size());
    }

    @Override
    public List<CallGraphFileInfo> getCallGraphFileInfoByRecordId(Long recordId) {
        return callGraphFileInfoMapper.findByRecordId(recordId);
    }

    @Override
    public List<CallGraphFileInfo> getCallGraphFileInfoByExecId(String execId) {
        CallGraphExecutionRecord record = callGraphExecutionRecordMapper.findByExecId(execId);
        if (record == null) {
            return java.util.Collections.emptyList();
        }
        return callGraphFileInfoMapper.findByRecordId(record.getId());
    }

    @Override
    public FindStackExecutionRecord saveFindStackRecord(FindStackExecutionRecord record) {
        findStackExecutionRecordMapper.insert(record);
        logger.info("保存关键字生成堆栈执行记录: id={}, execId={}", record.getId(), record.getExecId());
        return record;
    }

    @Override
    public void updateFindStackStatus(Long id, String status, Long duration, String outputDir, String errorMessage) {
        findStackExecutionRecordMapper.updateStatus(id, status, new Date(), duration, outputDir, errorMessage);
        logger.info("更新关键字生成堆栈执行状态: id={}, status={}, outputDir={}", id, status, outputDir);
    }

    @Override
    public Map<String, Object> queryFindStackRecords(String templateId, Date minStartTime, int pageNum, int pageSize) {
        if (minStartTime == null) {
            minStartTime = new Date(0);
        }

        int total = findStackExecutionRecordMapper.countByTemplateId(templateId, minStartTime);
        int offset = (pageNum - 1) * pageSize;
        List<FindStackExecutionRecord> records = findStackExecutionRecordMapper.findByTemplateIdWithPage(
                templateId, minStartTime, pageSize, offset);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    @Override
    public FindStackExecutionRecord getFindStackRecordById(Long id) {
        return findStackExecutionRecordMapper.findById(id);
    }
}
