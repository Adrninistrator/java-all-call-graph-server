package com.github.adrninistrator.jacgserver.service.impl;

import com.github.adrninistrator.jacgserver.mapper.AnalysisExecutionRecordMapper;
import com.github.adrninistrator.jacgserver.model.dto.ExecutionRecordQueryDTO;
import com.github.adrninistrator.jacgserver.model.entity.AnalysisExecutionRecord;
import com.github.adrninistrator.jacgserver.model.vo.ExecutionRecordVO;
import com.github.adrninistrator.jacgserver.service.ExecutionRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 执行记录服务实现类
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@Service
public class ExecutionRecordServiceImpl implements ExecutionRecordService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionRecordServiceImpl.class);

    @Resource
    private AnalysisExecutionRecordMapper analysisExecutionRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnalysisExecutionRecord saveRecord(AnalysisExecutionRecord record) {
        try {
            Date now = new Date();
            record.setCreateTime(now);
            record.setUpdateTime(now);
            analysisExecutionRecordMapper.insert(record);
            return record;
        } catch (Exception e) {
            logger.error("保存执行记录失败", e);
            throw new RuntimeException("保存执行记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status, Long duration, String errorMessage, String logFilePath) {
        try {
            analysisExecutionRecordMapper.updateStatus(id, status, new Date(), duration, errorMessage, logFilePath);
        } catch (Exception e) {
            logger.error("更新执行状态失败", e);
            throw new RuntimeException("更新执行状态失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ExecutionRecordVO queryRecords(ExecutionRecordQueryDTO queryDTO) {
        ExecutionRecordVO result = new ExecutionRecordVO();

        try {
            // 参数校验
            if (queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1) {
                queryDTO.setPageNum(1);
            }
            if (queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1) {
                queryDTO.setPageSize(10);
            }
            // 限制每页数量在10-50之间
            if (queryDTO.getPageSize() > 50) {
                queryDTO.setPageSize(50);
            }
            // 限制最多查询1000条
            int maxPage = (1000 + queryDTO.getPageSize() - 1) / queryDTO.getPageSize();
            if (queryDTO.getPageNum() > maxPage) {
                queryDTO.setPageNum(maxPage);
            }

            // 查询总数
            int total = analysisExecutionRecordMapper.countByProjectId(
                    queryDTO.getProjectId(),
                    queryDTO.getMinStartTime());

            // 计算分页
            int totalPages = (total + queryDTO.getPageSize() - 1) / queryDTO.getPageSize();
            int offset = (queryDTO.getPageNum() - 1) * queryDTO.getPageSize();

            // 查询记录列表
            List<AnalysisExecutionRecord> records = analysisExecutionRecordMapper.selectByProjectIdWithPage(
                    queryDTO.getProjectId(),
                    queryDTO.getMinStartTime(),
                    offset,
                    queryDTO.getPageSize());

            // 转换为VO
            List<ExecutionRecordVO.RecordItem> recordItems = new ArrayList<>();
            if (records != null && !records.isEmpty()) {
                recordItems = records.stream().map(this::convertToRecordItem).collect(Collectors.toList());
            }

            result.setRecords(recordItems);
            result.setTotal(total);
            result.setPageNum(queryDTO.getPageNum());
            result.setPageSize(queryDTO.getPageSize());
            result.setTotalPages(totalPages);

        } catch (Exception e) {
            logger.error("查询执行记录失败", e);
            throw new RuntimeException("查询执行记录失败: " + e.getMessage(), e);
        }

        return result;
    }

    @Override
    public AnalysisExecutionRecord getByExecId(String execId) {
        try {
            return analysisExecutionRecordMapper.selectByExecId(execId);
        } catch (Exception e) {
            logger.error("查询执行记录失败: execId={}", execId, e);
            return null;
        }
    }

    @Override
    public AnalysisExecutionRecord getById(Long id) {
        try {
            return analysisExecutionRecordMapper.selectById(id);
        } catch (Exception e) {
            logger.error("查询执行记录失败: id={}", id, e);
            return null;
        }
    }

    @Override
    public AnalysisExecutionRecord getLatestSuccessByProjectId(String projectId) {
        try {
            return analysisExecutionRecordMapper.selectLatestSuccessByProjectId(projectId);
        } catch (Exception e) {
            logger.error("查询项目最近成功执行记录失败: projectId={}", projectId, e);
            return null;
        }
    }

    /**
     * 转换为RecordItem
     */
    private ExecutionRecordVO.RecordItem convertToRecordItem(AnalysisExecutionRecord record) {
        ExecutionRecordVO.RecordItem item = new ExecutionRecordVO.RecordItem();
        item.setId(record.getId());
        item.setExecId(record.getExecId());
        item.setProjectId(record.getProjectId());
        item.setProjectDesc(record.getProjectDesc());
        item.setJarFiles(record.getJarFiles());
        item.setStartTime(record.getStartTime());
        item.setEndTime(record.getEndTime());
        item.setStatus(record.getStatus());
        item.setDuration(record.getDuration());
        item.setErrorMessage(record.getErrorMessage());
        item.setLogFilePath(record.getLogFilePath());
        return item;
    }
}
