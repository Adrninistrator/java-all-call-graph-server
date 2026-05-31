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
 * 模板配置参数专项测试
 * 分别验证JACG的Map/List/Set/EL表达式类型参数在模板中的创建、修改和查询
 * 模板只涉及java-all-call-graph组件的配置，不涉及java-callgraph2组件
 * EL表达式配置的value为String格式（非数组）
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TemplateConfigParamTest extends BaseTest {

    private static String testProjectId;
    private static String callerTemplateId;
    private static String calleeTemplateId;
    private static String elTemplateId;
    private static boolean projectCreated = false;

    /**
     * 确保测试项目已创建
     */
    private void ensureProjectCreated() throws Exception {
        if (projectCreated) {
            return;
        }

        Map<String, Object> projectBody = new LinkedHashMap<>();
        projectBody.put("description", "模板配置参数专项测试_" + System.currentTimeMillis());

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
     * 模板-JACG主配置(Map)：创建并验证
     */
    @Test
    @Order(1)
    void testTemplateJACGMainConfigCreate() throws Exception {
        ensureProjectCreated();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "模板主配置测试_caller");
        body.put("direction", "caller");
        body.put("defaultTemplate", false);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> mainConfig = new LinkedHashMap<>();
        mainConfig.put("CKE_CALL_GRAPH_OUTPUT_DETAIL", "2");
        mainConfig.put("CKE_THREAD_NUM", "15");
        jacgConfig.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), mainConfig);

        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("Class1.method1"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("jacgConfig", jacgConfig);

        MvcResult result = mockMvc.perform(post("/api/v1/projects/{projectId}/templates", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        callerTemplateId = extractJsonValue(result.getResponse().getContentAsString(), "$.data.templateId");

        // 查询验证 - Map配置的value为String格式
        mockMvc.perform(get("/api/v1/templates/{templateId}", callerTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_CALL_GRAPH_OUTPUT_DETAIL").value("2"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_THREAD_NUM").value("15"));
    }

    /**
     * 模板-JACG主配置(Map)：修改并验证
     */
    @Test
    @Order(2)
    void testTemplateJACGMainConfigUpdate() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "模板主配置测试_caller");
        body.put("direction", "caller");

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> mainConfig = new LinkedHashMap<>();
        mainConfig.put("CKE_CALL_GRAPH_OUTPUT_DETAIL", "1");
        mainConfig.put("CKE_IGNORE_DUP_CALLEE_IN_ONE_CALLER", true);
        jacgConfig.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), mainConfig);

        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("Class1.method1"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/templates/{templateId}", callerTemplateId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证 - Map配置修改后value仍为String格式
        mockMvc.perform(get("/api/v1/templates/{templateId}", callerTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_CALL_GRAPH_OUTPUT_DETAIL").value("1"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_IGNORE_DUP_CALLEE_IN_ONE_CALLER").value("true"));
    }

    /**
     * 模板-JACG List配置：创建并验证
     */
    @Test
    @Order(3)
    void testTemplateJACGListConfigCreate() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "模板List配置测试_callee");
        body.put("direction", "callee");
        body.put("defaultTemplate", false);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_FIND_STACK_KEYWORD_4EE", Arrays.asList("kw1", "kw2"));
        jacgConfig.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);

        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLEE", Arrays.asList("ClassA.methodA"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("jacgConfig", jacgConfig);

        MvcResult result = mockMvc.perform(post("/api/v1/projects/{projectId}/templates", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        calleeTemplateId = extractJsonValue(result.getResponse().getContentAsString(), "$.data.templateId");

        // 查询验证 - List配置的value为数组格式，顺序保留
        mockMvc.perform(get("/api/v1/templates/{templateId}", calleeTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4EE[0]").value("kw1"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4EE[1]").value("kw2"));
    }

    /**
     * 模板-JACG List配置：修改并验证
     */
    @Test
    @Order(4)
    void testTemplateJACGListConfigUpdate() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "模板List配置测试_callee");
        body.put("direction", "callee");

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_FIND_STACK_KEYWORD_4EE", Arrays.asList("kw3", "kw4", "kw5"));
        jacgConfig.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);

        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLEE", Arrays.asList("ClassA.methodA"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/templates/{templateId}", calleeTemplateId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证 - List配置修改后value仍为数组格式，顺序保留
        mockMvc.perform(get("/api/v1/templates/{templateId}", calleeTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4EE[0]").value("kw3"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4EE[1]").value("kw4"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4EE[2]").value("kw5"));
    }

    /**
     * 模板-JACG Set配置：创建并验证
     */
    @Test
    @Order(5)
    void testTemplateJACGSetConfigCreate() throws Exception {
        // 先删除旧的caller模板
        if (callerTemplateId != null) {
            try {
                mockMvc.perform(delete("/api/v1/templates/{templateId}", callerTemplateId))
                        .andExpect(status().isOk());
            } catch (Exception ignored) {
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "模板Set配置测试_caller");
        body.put("direction", "caller");
        body.put("defaultTemplate", false);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("C1.m1", "C2.m2"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("jacgConfig", jacgConfig);

        MvcResult result = mockMvc.perform(post("/api/v1/projects/{projectId}/templates", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        callerTemplateId = extractJsonValue(result.getResponse().getContentAsString(), "$.data.templateId");

        // 查询验证 - Set配置的value为数组格式
        mockMvc.perform(get("/api/v1/templates/{templateId}", callerTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.SET_CONFIG.getValue() + ".OCFUSE_METHOD_CLASS_4CALLER").isArray());
    }

    /**
     * 模板-JACG Set配置：修改并验证
     */
    @Test
    @Order(6)
    void testTemplateJACGSetConfigUpdate() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "模板Set配置测试_caller");
        body.put("direction", "caller");

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("C3.m3", "C4.m4"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/templates/{templateId}", callerTemplateId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证 - Set配置修改后value仍为数组格式
        mockMvc.perform(get("/api/v1/templates/{templateId}", callerTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.SET_CONFIG.getValue() + ".OCFUSE_METHOD_CLASS_4CALLER").isArray());
    }

    /**
     * 模板-JACG EL表达式配置：创建并验证
     * EL表达式的value为String格式（非数组）
     */
    @Test
    @Order(7)
    void testTemplateJACGElConfigCreate() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "模板EL配置测试_caller");
        body.put("direction", "caller");
        body.put("defaultTemplate", false);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> elConfig = new LinkedHashMap<>();
        elConfig.put("ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL", "true");
        jacgConfig.put(ConfigCategoryEnum.EL_CONFIG.getValue(), elConfig);

        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("C1.m1"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("jacgConfig", jacgConfig);

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/projects/{projectId}/templates", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        elTemplateId = extractJsonValue(mvcResult.getResponse().getContentAsString(), "$.data.templateId");

        // 查询验证 - EL表达式配置的value为String格式，不是数组
        mockMvc.perform(get("/api/v1/templates/{templateId}", elTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.EL_CONFIG.getValue() + ".ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL").value("true"))
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
                    com.fasterxml.jackson.databind.JsonNode elValue = jsonNode.at("/data/jacgConfig/" + ConfigCategoryEnum.EL_CONFIG.getValue() + "/ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL");
                    // EL表达式value必须是String，不能是数组
                    org.junit.jupiter.api.Assertions.assertFalse(elValue.isArray(),
                            "EL表达式配置的value应为String格式，不应为数组，实际: " + elValue.getNodeType());
                    org.junit.jupiter.api.Assertions.assertEquals("true", elValue.asText(),
                            "EL表达式配置的value应为\"true\"");
                });
    }

    /**
     * 模板-JACG EL表达式配置：修改并验证
     * EL表达式的value为String格式（非数组）
     */
    @Test
    @Order(8)
    void testTemplateJACGElConfigUpdate() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "模板EL配置测试_caller");
        body.put("direction", "caller");

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> elConfig = new LinkedHashMap<>();
        elConfig.put("ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL", "false");
        jacgConfig.put(ConfigCategoryEnum.EL_CONFIG.getValue(), elConfig);

        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("C1.m1"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/templates/{templateId}", elTemplateId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证 - EL表达式配置修改后value仍为String格式，不是数组
        mockMvc.perform(get("/api/v1/templates/{templateId}", elTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.EL_CONFIG.getValue() + ".ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL").value("false"))
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
                    com.fasterxml.jackson.databind.JsonNode elValue = jsonNode.at("/data/jacgConfig/" + ConfigCategoryEnum.EL_CONFIG.getValue() + "/ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL");
                    // EL表达式value必须是String，不能是数组
                    org.junit.jupiter.api.Assertions.assertFalse(elValue.isArray(),
                            "EL表达式配置的value应为String格式，不应为数组，实际: " + elValue.getNodeType());
                    org.junit.jupiter.api.Assertions.assertEquals("false", elValue.asText(),
                            "EL表达式配置的value应为\"false\"");
                });
    }

    /**
     * 模板-数据库配置跟随项目
     */
    @Test
    @Order(9)
    void testTemplateDbConfigFollowsProject() throws Exception {
        // 先修改项目的数据库配置
        Map<String, Object> projectBody = new LinkedHashMap<>();
        projectBody.put("description", "模板配置参数专项测试_" + testProjectId);

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_JAR_DIR", Arrays.asList("/path/to/test.jar"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);
        projectBody.put("javacg2Config", javacg2Config);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> dbConfig = new LinkedHashMap<>();
        dbConfig.put("CDKE_DB_USE_H2", true);
        jacgConfig.put(ConfigCategoryEnum.DB_CONFIG.getValue(), dbConfig);
        projectBody.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/projects/{projectId}", testProjectId)
                        .contentType("application/json")
                        .content(toJson(projectBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 更新模板（触发数据库配置同步）
        Map<String, Object> templateBody = new LinkedHashMap<>();
        templateBody.put("description", "模板Set配置测试_caller");
        templateBody.put("direction", "caller");

        Map<String, Object> templateJacgConfig = new LinkedHashMap<>();
        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("C3.m3"));
        templateJacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);
        templateBody.put("jacgConfig", templateJacgConfig);

        mockMvc.perform(put("/api/v1/templates/{templateId}", callerTemplateId)
                        .contentType("application/json")
                        .content(toJson(templateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询模板验证数据库配置
        mockMvc.perform(get("/api/v1/templates/{templateId}", callerTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.DB_CONFIG.getValue()).exists());
    }

    /**
     * 模板-数据库配置参数由后端自动使用项目的数据库配置覆盖
     * 更新模板时若指定dbConfig，后端会忽略并自动使用项目的数据库配置
     */
    @Test
    @Order(10)
    void testTemplateDbConfigForbidden() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "模板Set配置测试_caller");
        body.put("direction", "caller");

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> dbConfig = new LinkedHashMap<>();
        dbConfig.put("CDKE_DB_USE_H2", true);
        jacgConfig.put(ConfigCategoryEnum.DB_CONFIG.getValue(), dbConfig);

        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("C1.m1"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/templates/{templateId}", callerTemplateId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 默认模板-禁止修改入口类/方法配置
     * 创建向上默认模板后，尝试修改OCFUSE_METHOD_CLASS_4CALLEE，应返回错误
     */
    @Test
    @Order(11)
    void testDefaultTemplateModifyCalleeEntryPointForbidden() throws Exception {
        // 创建向上默认模板
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

        String defaultCalleeId = extractJsonValue(result.getResponse().getContentAsString(), "$.data.templateId");

        // 尝试修改默认模板的入口类/方法
        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("description", "default_template_4ee");
        updateBody.put("direction", "callee");

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLEE", Arrays.asList("com.test.ForbiddenClass"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);
        updateBody.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/templates/{templateId}", defaultCalleeId)
                        .contentType("application/json")
                        .content(toJson(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003))
                .andExpect(jsonPath("$.message").value("默认模板的入口类/方法配置不允许修改"));

        // 清理：删除默认模板
        mockMvc.perform(delete("/api/v1/templates/{templateId}", defaultCalleeId))
                .andExpect(status().isOk());
    }

    /**
     * 默认模板-禁止修改入口类/方法配置
     * 创建向下默认模板后，尝试修改OCFUSE_METHOD_CLASS_4CALLER，应返回错误
     */
    @Test
    @Order(12)
    void testDefaultTemplateModifyCallerEntryPointForbidden() throws Exception {
        // 创建向下默认模板
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

        String defaultCallerId = extractJsonValue(result.getResponse().getContentAsString(), "$.data.templateId");

        // 尝试修改默认模板的入口类/方法
        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("description", "default_template_4er");
        updateBody.put("direction", "caller");

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("com.test.ForbiddenClass"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);
        updateBody.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/templates/{templateId}", defaultCallerId)
                        .contentType("application/json")
                        .content(toJson(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003))
                .andExpect(jsonPath("$.message").value("默认模板的入口类/方法配置不允许修改"));

        // 清理：删除默认模板
        mockMvc.perform(delete("/api/v1/templates/{templateId}", defaultCallerId))
                .andExpect(status().isOk());
    }

    /**
     * 默认模板-不修改入口类/方法，修改其他配置应成功
     */
    @Test
    @Order(13)
    void testDefaultTemplateModifyOtherConfigSuccess() throws Exception {
        // 创建向下默认模板
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("direction", "caller");
        body.put("defaultTemplate", true);

        MvcResult result = mockMvc.perform(post("/api/v1/projects/{projectId}/templates", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String defaultCallerId = extractJsonValue(result.getResponse().getContentAsString(), "$.data.templateId");

        // 不修改入口类/方法，修改主配置
        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("description", "default_template_4er");
        updateBody.put("direction", "caller");

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> mainConfig = new LinkedHashMap<>();
        mainConfig.put("CKE_CALL_GRAPH_OUTPUT_DETAIL", "2");
        jacgConfig.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), mainConfig);

        // 保持入口类/方法为占位符
        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("${place_holder}"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);
        updateBody.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/templates/{templateId}", defaultCallerId)
                        .contentType("application/json")
                        .content(toJson(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证修改生效
        mockMvc.perform(get("/api/v1/templates/{templateId}", defaultCallerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_CALL_GRAPH_OUTPUT_DETAIL").value("2"));

        // 清理：删除默认模板
        mockMvc.perform(delete("/api/v1/templates/{templateId}", defaultCallerId))
                .andExpect(status().isOk());
    }

    /**
     * 清理：删除测试模板和项目
     */
    @Test
    @Order(99)
    void testCleanup() throws Exception {
        if (calleeTemplateId != null) {
            mockMvc.perform(delete("/api/v1/templates/{templateId}", calleeTemplateId))
                    .andExpect(status().isOk());
        }
        if (elTemplateId != null) {
            mockMvc.perform(delete("/api/v1/templates/{templateId}", elTemplateId))
                    .andExpect(status().isOk());
        }
        if (callerTemplateId != null) {
            mockMvc.perform(delete("/api/v1/templates/{templateId}", callerTemplateId))
                    .andExpect(status().isOk());
        }
        if (testProjectId != null) {
            mockMvc.perform(delete("/api/v1/projects/{projectId}", testProjectId))
                    .andExpect(status().isOk());
        }
    }
}
