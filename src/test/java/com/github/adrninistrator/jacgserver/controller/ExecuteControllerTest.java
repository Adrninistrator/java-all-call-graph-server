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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 执行控制器测试
 * 包含使用当前项目编译后的class目录进行真实的静态分析和调用链生成测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExecuteControllerTest extends BaseTest {

    private static String testProjectId;
    private static String testCallerTemplateId;
    private static String testCalleeTemplateId;
    private static boolean initialized = false;

    /**
     * 创建测试项目，使用编译后的class目录作为静态分析的jar文件/目录
     */
    private void ensureInitialized() throws Exception {
        if (initialized) {
            return;
        }

        // 使用编译后的class目录作为静态分析的jar文件/目录
        String classDir = getClassDirPath();
        assertNotNull(classDir, "未找到编译后的class目录");

        // 创建测试项目，使用class目录路径
        Map<String, Object> projectBody = new LinkedHashMap<>();
        projectBody.put("description", "执行测试项目_" + System.currentTimeMillis());

        // JavaCG2配置 - 指定class目录路径
        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_JAR_DIR", Arrays.asList(classDir));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);
        projectBody.put("javacg2Config", javacg2Config);

        // JACG配置 - 必须提供，否则执行分析时_jacg_config目录不存在会导致失败
        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> jacgDbConfig = new LinkedHashMap<>();
        jacgDbConfig.put("CDKE_DB_USE_H2", true);
        jacgConfig.put(ConfigCategoryEnum.DB_CONFIG.getValue(), jacgDbConfig);
        Map<String, Object> jacgMainConfig = new LinkedHashMap<>();
        jacgMainConfig.put("CKE_CALL_GRAPH_OUTPUT_DETAIL", "1");
        jacgConfig.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), jacgMainConfig);
        projectBody.put("jacgConfig", jacgConfig);

        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .contentType("application/json")
                        .content(toJson(projectBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        testProjectId = extractJsonValue(result.getResponse().getContentAsString(), "$.data.projectId");
        waitForIdGeneration();

        // 创建向下(caller)模板
        // 使用当前项目中存在的类与方法，输出内容数量适中
        Map<String, Object> callerTemplateBody = new LinkedHashMap<>();
        callerTemplateBody.put("description", "执行测试模板_caller");
        callerTemplateBody.put("direction", "caller");
        callerTemplateBody.put("defaultTemplate", false);

        Map<String, Object> callerJacgConfig = new LinkedHashMap<>();
        Map<String, Object> callerMainConfig = new LinkedHashMap<>();
        callerMainConfig.put("CKE_CALL_GRAPH_OUTPUT_DETAIL", "1");
        callerJacgConfig.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), callerMainConfig);

        Map<String, List<String>> callerSetConfig = new LinkedHashMap<>();
        // 选择当前项目中输出内容适中的类作为向下调用链入口
        callerSetConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList(
                "com.github.adrninistrator.jacgserver.controller.ConfigController"
        ));
        callerJacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), callerSetConfig);

        callerTemplateBody.put("jacgConfig", callerJacgConfig);

        MvcResult callerResult = mockMvc.perform(
                        post("/api/v1/projects/{projectId}/templates", testProjectId)
                                .contentType("application/json")
                                .content(toJson(callerTemplateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        testCallerTemplateId = extractJsonValue(callerResult.getResponse().getContentAsString(), "$.data.templateId");
        waitForIdGeneration();

        // 创建向上(callee)模板
        Map<String, Object> calleeTemplateBody = new LinkedHashMap<>();
        calleeTemplateBody.put("description", "执行测试模板_callee");
        calleeTemplateBody.put("direction", "callee");
        calleeTemplateBody.put("defaultTemplate", false);

        Map<String, Object> calleeJacgConfig = new LinkedHashMap<>();
        Map<String, Object> calleeMainConfig = new LinkedHashMap<>();
        calleeMainConfig.put("CKE_CALL_GRAPH_OUTPUT_DETAIL", "1");
        calleeJacgConfig.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), calleeMainConfig);

        Map<String, List<String>> calleeSetConfig = new LinkedHashMap<>();
        // 选择当前项目中输出内容适中的类作为向上调用链入口
        // OCFUSE_METHOD_CLASS_4CALLEE 需要指定类名（不含方法名）
        calleeSetConfig.put("OCFUSE_METHOD_CLASS_4CALLEE", Arrays.asList(
                "com.github.adrninistrator.jacgserver.util.IdGenerator"
        ));
        calleeJacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), calleeSetConfig);

        calleeTemplateBody.put("jacgConfig", calleeJacgConfig);

        MvcResult calleeResult = mockMvc.perform(
                        post("/api/v1/projects/{projectId}/templates", testProjectId)
                                .contentType("application/json")
                                .content(toJson(calleeTemplateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        testCalleeTemplateId = extractJsonValue(calleeResult.getResponse().getContentAsString(), "$.data.templateId");
        initialized = true;
    }

    @Test
    @Order(1)
    void testExecuteAnalysisProjectNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{projectId}/execute/analysis", "nonexistent_project_id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @Order(2)
    void testExecuteCallGraphTemplateNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/templates/{templateId}/execute/callgraph", "nonexistent_template_id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    @Order(3)
    void testExecuteFindStackTemplateNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/templates/{templateId}/execute/findstack", "nonexistent_template_id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    @Order(4)
    void testIsProjectExecuting() throws Exception {
        ensureInitialized();

        mockMvc.perform(get("/api/v1/projects/{projectId}/executing", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.executing").value(false));
    }

    @Test
    @Order(5)
    void testIsTemplateExecuting() throws Exception {
        ensureInitialized();

        mockMvc.perform(get("/api/v1/templates/{templateId}/executing", testCallerTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.executing").value(false));
    }

    @Test
    @Order(6)
    void testGetExecutionStatusNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/executions/{execId}/status", "nonexistent_exec_id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    @Order(7)
    void testGetExecutionLogsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/executions/{execId}/logs", "nonexistent_exec_id")
                        .param("lines", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    @Order(10)
    void testExecuteAnalysis() throws Exception {
        ensureInitialized();

        MvcResult result = mockMvc.perform(post("/api/v1/projects/{projectId}/execute/analysis", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.execId").exists())
                .andExpect(jsonPath("$.data.status").value("running"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String execId = extractJsonValue(response, "$.data.execId");

        // 等待静态分析完成（最多120秒）
        String finalStatus = waitForExecutionComplete(execId, 120000);
        assertNotNull(finalStatus, "静态分析执行超时");
        assertEquals("completed", finalStatus, "静态分析执行未成功完成，状态: " + finalStatus);
    }

    @Test
    @Order(11)
    void testExecuteCallGraph4Caller() throws Exception {
        ensureInitialized();

        MvcResult result = mockMvc.perform(post("/api/v1/templates/{templateId}/execute/callgraph", testCallerTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.execId").exists())
                .andExpect(jsonPath("$.data.status").value("running"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String execId = extractJsonValue(response, "$.data.execId");

        // 等待调用链生成完成（最多120秒）
        String finalStatus = waitForExecutionComplete(execId, 120000);
        assertNotNull(finalStatus, "向下调用链生成执行超时");
        assertEquals("completed", finalStatus, "向下调用链生成未成功完成，状态: " + finalStatus);
    }

    @Test
    @Order(12)
    void testExecuteCallGraph4Callee() throws Exception {
        ensureInitialized();

        MvcResult result = mockMvc.perform(post("/api/v1/templates/{templateId}/execute/callgraph", testCalleeTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.execId").exists())
                .andExpect(jsonPath("$.data.status").value("running"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String execId = extractJsonValue(response, "$.data.execId");

        // 等待调用链生成完成（最多120秒）
        String finalStatus = waitForExecutionComplete(execId, 120000);
        assertNotNull(finalStatus, "向上调用链生成执行超时");
        assertEquals("completed", finalStatus, "向上调用链生成未成功完成，状态: " + finalStatus);
    }

    @Test
    @Order(13)
    void testExecuteCallGraphWithoutAnalysis() throws Exception {
        // 创建一个新项目但不执行分析，然后尝试生成调用链，应该失败
        String classDir = getClassDirPath();
        Map<String, Object> projectBody = new LinkedHashMap<>();
        projectBody.put("description", "未分析项目_" + System.currentTimeMillis());

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_JAR_DIR", Arrays.asList(classDir));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);
        projectBody.put("javacg2Config", javacg2Config);

        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                        .contentType("application/json")
                        .content(toJson(projectBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String newProjectId = extractJsonValue(projectResult.getResponse().getContentAsString(), "$.data.projectId");
        waitForIdGeneration();

        // 创建模板
        Map<String, Object> templateBody = new LinkedHashMap<>();
        templateBody.put("description", "未分析模板_" + System.currentTimeMillis());
        templateBody.put("direction", "caller");
        templateBody.put("defaultTemplate", false);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("com.github.adrninistrator.jacgserver.controller.ConfigController"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);
        templateBody.put("jacgConfig", jacgConfig);

        MvcResult templateResult = mockMvc.perform(post("/api/v1/projects/{projectId}/templates", newProjectId)
                        .contentType("application/json")
                        .content(toJson(templateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String newTemplateId = extractJsonValue(templateResult.getResponse().getContentAsString(), "$.data.templateId");

        // 不执行分析直接生成调用链，应返回错误
        mockMvc.perform(post("/api/v1/templates/{templateId}/execute/callgraph", newTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1004));

        // 清理
        mockMvc.perform(delete("/api/v1/templates/{templateId}", newTemplateId))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/projects/{projectId}", newProjectId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(14)
    void testGetExecutionStatusAndLogs() throws Exception {
        ensureInitialized();

        // 执行分析
        MvcResult result = mockMvc.perform(post("/api/v1/projects/{projectId}/execute/analysis", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String execId = extractJsonValue(response, "$.data.execId");

        // 查询执行状态（running）
        mockMvc.perform(get("/api/v1/executions/{execId}/status", execId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.execId").value(execId));

        // 等待完成
        String finalStatus = waitForExecutionComplete(execId, 120000);
        assertNotNull(finalStatus, "静态分析执行超时");

        // 查询完成后的状态
        mockMvc.perform(get("/api/v1/executions/{execId}/status", execId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("completed"));

        // 查询执行日志
        mockMvc.perform(get("/api/v1/executions/{execId}/logs", execId)
                        .param("lines", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.logs").isArray());
    }

    @Test
    @Order(99)
    void testCleanup() throws Exception {
        ensureInitialized();
        if (testCallerTemplateId != null) {
            mockMvc.perform(delete("/api/v1/templates/{templateId}", testCallerTemplateId))
                    .andExpect(status().isOk());
        }
        if (testCalleeTemplateId != null) {
            mockMvc.perform(delete("/api/v1/templates/{templateId}", testCalleeTemplateId))
                    .andExpect(status().isOk());
        }
        if (testProjectId != null) {
            mockMvc.perform(delete("/api/v1/projects/{projectId}", testProjectId))
                    .andExpect(status().isOk());
        }
    }
}
