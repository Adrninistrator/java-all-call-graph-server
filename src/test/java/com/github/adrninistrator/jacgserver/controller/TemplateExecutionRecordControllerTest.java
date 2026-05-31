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
 * 模板执行记录控制器测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TemplateExecutionRecordControllerTest extends BaseTest {

    private static String testProjectId;
    private static String testTemplateId;
    private static boolean initialized = false;

    /**
     * 在第一个测试方法中初始化测试数据
     */
    private void ensureInitialized() throws Exception {
        if (initialized) {
            return;
        }

        // 创建测试项目
        Map<String, Object> projectBody = new LinkedHashMap<>();
        projectBody.put("description", "模板执行记录测试项目_" + System.currentTimeMillis());

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_JAR_DIR", Arrays.asList("/path/to/test.jar"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);
        projectBody.put("javacg2Config", javacg2Config);

        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                        .contentType("application/json")
                        .content(toJson(projectBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        testProjectId = extractJsonValue(projectResult.getResponse().getContentAsString(), "$.data.projectId");
        waitForIdGeneration();

        // 创建测试模板
        Map<String, Object> templateBody = new LinkedHashMap<>();
        templateBody.put("description", "模板执行记录测试模板");
        templateBody.put("direction", "caller");
        templateBody.put("defaultTemplate", false);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("Class1.method1"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);
        templateBody.put("jacgConfig", jacgConfig);

        MvcResult templateResult = mockMvc.perform(post("/api/v1/projects/{projectId}/templates", testProjectId)
                        .contentType("application/json")
                        .content(toJson(templateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        testTemplateId = extractJsonValue(templateResult.getResponse().getContentAsString(), "$.data.templateId");
        initialized = true;
    }

    /**
     * 测试查询调用链执行记录（无记录）
     */
    @Test
    @Order(1)
    void testQueryCallGraphRecordsEmpty() throws Exception {
        ensureInitialized();

        mockMvc.perform(get("/api/v1/template/execution/call-graph/records/{templateId}", testTemplateId)
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").exists());
    }

    /**
     * 测试查询调用链执行记录（带时间过滤）
     */
    @Test
    @Order(2)
    void testQueryCallGraphRecordsWithTimeFilter() throws Exception {
        mockMvc.perform(get("/api/v1/template/execution/call-graph/records/{templateId}", testTemplateId)
                        .param("minStartTime", "2020-01-01 00:00:00")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 测试查询调用链执行记录详情（不存在）
     */
    @Test
    @Order(3)
    void testGetCallGraphDetailNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/template/execution/call-graph/detail/{id}", 999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 测试查询关键字生成堆栈执行记录（无记录）
     */
    @Test
    @Order(4)
    void testQueryFindStackRecordsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/template/execution/find-stack/records/{templateId}", testTemplateId)
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").exists());
    }

    /**
     * 测试查询关键字生成堆栈执行记录（带时间过滤）
     */
    @Test
    @Order(5)
    void testQueryFindStackRecordsWithTimeFilter() throws Exception {
        mockMvc.perform(get("/api/v1/template/execution/find-stack/records/{templateId}", testTemplateId)
                        .param("minStartTime", "2020-01-01 00:00:00")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 测试查询关键字生成堆栈执行记录详情（不存在）
     */
    @Test
    @Order(6)
    void testGetFindStackDetailNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/template/execution/find-stack/detail/{id}", 999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 清理：删除测试模板和项目
     */
    @Test
    @Order(99)
    void testCleanup() throws Exception {
        ensureInitialized();
        if (testTemplateId != null) {
            mockMvc.perform(delete("/api/v1/templates/{templateId}", testTemplateId))
                    .andExpect(status().isOk());
        }
        if (testProjectId != null) {
            mockMvc.perform(delete("/api/v1/projects/{projectId}", testProjectId))
                    .andExpect(status().isOk());
        }
    }
}
