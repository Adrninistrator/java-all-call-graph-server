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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 模板控制器测试
 * 包含模板CRUD、JACG配置参数保存与验证
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TemplateControllerTest extends BaseTest {

    private static String testProjectId;
    private static String callerTemplateId;
    private static String calleeTemplateId;
    private static String copiedTemplateId;
    private static String defaultCalleeTemplateId;
    private static String defaultCallerTemplateId;
    private static boolean projectCreated = false;

    /**
     * 创建测试项目（在第一个测试方法中调用）
     */
    private void ensureProjectCreated() throws Exception {
        if (projectCreated) {
            return;
        }

        Map<String, Object> projectBody = new LinkedHashMap<>();
        projectBody.put("description", "模板测试项目_" + System.currentTimeMillis());

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
     * 创建向下模板的请求体（含完整JACG配置）
     */
    private String buildCreateCallerTemplateBody(String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", description);
        body.put("direction", "caller");
        body.put("defaultTemplate", false);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> mainConfig = new LinkedHashMap<>();
        mainConfig.put("CKE_CALL_GRAPH_OUTPUT_DETAIL", "1");
        jacgConfig.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), mainConfig);

        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_FIND_STACK_KEYWORD_4ER", Arrays.asList("keyword1"));
        jacgConfig.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);

        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("Class1.method1"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        Map<String, Object> elConfig = new LinkedHashMap<>();
        elConfig.put("ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL", "false");
        jacgConfig.put(ConfigCategoryEnum.EL_CONFIG.getValue(), elConfig);

        body.put("jacgConfig", jacgConfig);
        return toJson(body);
    }

    /**
     * 创建向上模板的请求体
     */
    private String buildCreateCalleeTemplateBody(String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", description);
        body.put("direction", "callee");
        body.put("defaultTemplate", false);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> mainConfig = new LinkedHashMap<>();
        mainConfig.put("CKE_CALL_GRAPH_OUTPUT_DETAIL", "1");
        jacgConfig.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), mainConfig);

        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_FIND_STACK_KEYWORD_4EE", Arrays.asList("keywordA"));
        jacgConfig.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);

        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLEE", Arrays.asList("ClassA.methodA"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        Map<String, Object> elConfig = new LinkedHashMap<>();
        elConfig.put("ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL", "false");
        jacgConfig.put(ConfigCategoryEnum.EL_CONFIG.getValue(), elConfig);

        body.put("jacgConfig", jacgConfig);
        return toJson(body);
    }

    @Test
    @Order(1)
    void testCreateCallerTemplateWithConfig() throws Exception {
        ensureProjectCreated();

        String body = buildCreateCallerTemplateBody("测试模板_caller");
        MvcResult result = mockMvc.perform(post("/api/v1/projects/{projectId}/templates", testProjectId)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templateId").exists())
                .andExpect(jsonPath("$.data.direction").value("caller"))
                .andReturn();

        callerTemplateId = extractJsonValue(result.getResponse().getContentAsString(), "$.data.templateId");
    }

    @Test
    @Order(2)
    void testGetTemplateVerifyConfig() throws Exception {
        mockMvc.perform(get("/api/v1/templates/{templateId}", callerTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_CALL_GRAPH_OUTPUT_DETAIL").value("1"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4ER").isArray())
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.SET_CONFIG.getValue() + ".OCFUSE_METHOD_CLASS_4CALLER").isArray())
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.EL_CONFIG.getValue() + ".ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL").value("false"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.DB_CONFIG.getValue()).exists());
    }

    @Test
    @Order(3)
    void testUpdateTemplateConfig() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "测试模板_caller");
        body.put("direction", "caller");

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> mainConfig = new LinkedHashMap<>();
        mainConfig.put("CKE_CALL_GRAPH_OUTPUT_DETAIL", "2");
        mainConfig.put("CKE_IGNORE_DUP_CALLEE_IN_ONE_CALLER", true);
        jacgConfig.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), mainConfig);

        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_FIND_STACK_KEYWORD_4ER", Arrays.asList("keyword2", "keyword3"));
        jacgConfig.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);

        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("Class2.method2", "Class3.method3"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        Map<String, Object> elConfig = new LinkedHashMap<>();
        elConfig.put("ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL", "true");
        jacgConfig.put(ConfigCategoryEnum.EL_CONFIG.getValue(), elConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/templates/{templateId}", callerTemplateId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(4)
    void testGetTemplateVerifyUpdatedConfig() throws Exception {
        mockMvc.perform(get("/api/v1/templates/{templateId}", callerTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_CALL_GRAPH_OUTPUT_DETAIL").value("2"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_IGNORE_DUP_CALLEE_IN_ONE_CALLER").value("true"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4ER[0]").value("keyword2"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4ER[1]").value("keyword3"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.SET_CONFIG.getValue() + ".OCFUSE_METHOD_CLASS_4CALLER").isArray())
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.EL_CONFIG.getValue() + ".ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL").value("true"));
    }

    @Test
    @Order(5)
    void testCreateCalleeTemplateWithConfig() throws Exception {
        String body = buildCreateCalleeTemplateBody("测试模板_callee");
        MvcResult result = mockMvc.perform(post("/api/v1/projects/{projectId}/templates", testProjectId)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.direction").value("callee"))
                .andReturn();

        calleeTemplateId = extractJsonValue(result.getResponse().getContentAsString(), "$.data.templateId");
    }

    @Test
    @Order(6)
    void testGetCalleeTemplateVerifyConfig() throws Exception {
        mockMvc.perform(get("/api/v1/templates/{templateId}", calleeTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.SET_CONFIG.getValue() + ".OCFUSE_METHOD_CLASS_4CALLEE").isArray())
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4EE").isArray());
    }

    @Test
    @Order(7)
    void testListTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{projectId}/templates", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templates").isArray());
    }

    @Test
    @Order(8)
    void testCopyTemplate() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "复制的测试模板");

        MvcResult result = mockMvc.perform(post("/api/v1/templates/{templateId}/copy", callerTemplateId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templateId").exists())
                .andReturn();

        copiedTemplateId = extractJsonValue(result.getResponse().getContentAsString(), "$.data.templateId");
    }

    @Test
    @Order(9)
    void testVerifyCopiedTemplateConfig() throws Exception {
        mockMvc.perform(get("/api/v1/templates/{templateId}", copiedTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_CALL_GRAPH_OUTPUT_DETAIL").value("2"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.EL_CONFIG.getValue() + ".ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL").value("true"));
    }

    @Test
    @Order(10)
    void testGetNonExistentTemplate() throws Exception {
        mockMvc.perform(get("/api/v1/templates/{templateId}", "nonexistent_template_id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    @Order(11)
    void testCreateTemplateProjectNotFound() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "测试模板");
        body.put("direction", "caller");

        mockMvc.perform(post("/api/v1/projects/{projectId}/templates", "nonexistent_project")
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @Order(12)
    void testCreateTemplateEmptyDescription() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "");
        body.put("direction", "caller");

        mockMvc.perform(post("/api/v1/projects/{projectId}/templates", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    @Order(13)
    void testCreateTemplateNoEntryPoints() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "测试模板_无入口_" + System.currentTimeMillis());
        body.put("direction", "caller");
        body.put("defaultTemplate", false);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        // 不设置入口类/方法
        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(post("/api/v1/projects/{projectId}/templates", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    @Order(14)
    void testCreateDefaultCalleeTemplate() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("direction", "callee");
        body.put("defaultTemplate", true);

        MvcResult result = mockMvc.perform(post("/api/v1/projects/{projectId}/templates", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templateId").value("default_template_4ee"))
                .andExpect(jsonPath("$.data.defaultTemplate").value(true))
                .andReturn();

        defaultCalleeTemplateId = extractJsonValue(result.getResponse().getContentAsString(), "$.data.templateId");
    }

    @Test
    @Order(15)
    void testCreateDefaultCallerTemplate() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("direction", "caller");
        body.put("defaultTemplate", true);

        MvcResult result = mockMvc.perform(post("/api/v1/projects/{projectId}/templates", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templateId").value("default_template_4er"))
                .andExpect(jsonPath("$.data.defaultTemplate").value(true))
                .andReturn();

        defaultCallerTemplateId = extractJsonValue(result.getResponse().getContentAsString(), "$.data.templateId");
    }

    @Test
    @Order(16)
    void testDuplicateDefaultCalleeTemplate() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("direction", "callee");
        body.put("defaultTemplate", true);

        mockMvc.perform(post("/api/v1/projects/{projectId}/templates", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    /**
     * 更新默认模板-尝试修改向上默认模板的入口类/方法，应返回错误
     */
    @Test
    @Order(17)
    void testUpdateDefaultCalleeTemplateModifyEntryPoint() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "default_template_4ee");
        body.put("direction", "callee");

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        // 尝试修改默认模板的入口类/方法
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLEE", Arrays.asList("com.test.ModifiedClass"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/templates/{templateId}", defaultCalleeTemplateId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003))
                .andExpect(jsonPath("$.message").value("默认模板的入口类/方法配置不允许修改"));
    }

    /**
     * 更新默认模板-尝试修改向下默认模板的入口类/方法，应返回错误
     */
    @Test
    @Order(18)
    void testUpdateDefaultCallerTemplateModifyEntryPoint() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "default_template_4er");
        body.put("direction", "caller");

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        // 尝试修改默认模板的入口类/方法
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("com.test.ModifiedClass"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/templates/{templateId}", defaultCallerTemplateId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003))
                .andExpect(jsonPath("$.message").value("默认模板的入口类/方法配置不允许修改"));
    }

    /**
     * 更新默认模板-不修改入口类/方法，修改其他配置，应成功
     */
    @Test
    @Order(19)
    void testUpdateDefaultTemplateWithoutModifyingEntryPoint() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "default_template_4er");
        body.put("direction", "caller");

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> mainConfig = new LinkedHashMap<>();
        mainConfig.put("CKE_CALL_GRAPH_OUTPUT_DETAIL", "2");
        jacgConfig.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), mainConfig);

        // 不修改入口类/方法配置
        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("${place_holder}"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/templates/{templateId}", defaultCallerTemplateId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(90)
    void testDeleteTemplates() throws Exception {
        // 删除复制的模板
        if (copiedTemplateId != null) {
            mockMvc.perform(delete("/api/v1/templates/{templateId}", copiedTemplateId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
        // 删除caller模板
        if (callerTemplateId != null) {
            mockMvc.perform(delete("/api/v1/templates/{templateId}", callerTemplateId))
                    .andExpect(status().isOk());
        }
        // 删除callee模板
        if (calleeTemplateId != null) {
            mockMvc.perform(delete("/api/v1/templates/{templateId}", calleeTemplateId))
                    .andExpect(status().isOk());
        }
        // 删除默认模板
        if (defaultCalleeTemplateId != null) {
            mockMvc.perform(delete("/api/v1/templates/{templateId}", defaultCalleeTemplateId))
                    .andExpect(status().isOk());
        }
        if (defaultCallerTemplateId != null) {
            mockMvc.perform(delete("/api/v1/templates/{templateId}", defaultCallerTemplateId))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @Order(91)
    void testDeleteProject() throws Exception {
        if (testProjectId != null) {
            mockMvc.perform(delete("/api/v1/projects/{projectId}", testProjectId))
                    .andExpect(status().isOk());
        }
    }
}
