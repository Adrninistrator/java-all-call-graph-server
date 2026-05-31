package com.github.adrninistrator.jacgserver.controller;

import com.github.adrninistrator.jacgserver.BaseTest;
import com.github.adrninistrator.jacgserver.constant.ConfigCategoryEnum;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 执行记录控制器测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExecutionRecordControllerTest extends BaseTest {

    private static String testProjectId;
    private static boolean projectCreated = false;

    /**
     * 确保测试项目已创建
     */
    private void ensureProjectCreated() throws Exception {
        if (projectCreated) {
            return;
        }

        Map<String, Object> projectBody = new LinkedHashMap<>();
        projectBody.put("description", "执行记录测试项目_" + System.currentTimeMillis());

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_JAR_DIR", Arrays.asList("/path/to/test.jar"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);
        projectBody.put("javacg2Config", javacg2Config);

        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .contentType("application/json")
                        .content(toJson(projectBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        testProjectId = extractJsonValue(result.getResponse().getContentAsString(), "$.data.projectId");
        projectCreated = true;
    }

    /**
     * 测试查询项目执行记录（无记录）
     */
    @Test
    @Order(1)
    void testQueryRecordsEmpty() throws Exception {
        ensureProjectCreated();

        mockMvc.perform(get("/api/v1/execution-records/project/{projectId}", testProjectId)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").exists());
    }

    /**
     * 测试查询项目执行记录（带时间过滤）
     */
    @Test
    @Order(2)
    void testQueryRecordsWithTimeFilter() throws Exception {
        mockMvc.perform(get("/api/v1/execution-records/project/{projectId}", testProjectId)
                        .param("minStartTime", "2020-01-01 00:00:00")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 测试查询项目执行记录（自定义分页）
     */
    @Test
    @Order(3)
    void testQueryRecordsCustomPaging() throws Exception {
        mockMvc.perform(get("/api/v1/execution-records/project/{projectId}", testProjectId)
                        .param("pageNum", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试获取执行记录详情（不存在）
     */
    @Test
    @Order(4)
    void testGetDetailNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/execution-records/{id}", 999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 清理：删除测试项目
     */
    @Test
    @Order(99)
    void testCleanup() throws Exception {
        if (testProjectId != null) {
            mockMvc.perform(delete("/api/v1/projects/{projectId}", testProjectId))
                    .andExpect(status().isOk());
        }
    }
}
