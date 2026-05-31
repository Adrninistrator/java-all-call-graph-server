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
 * 项目配置参数专项测试
 * 分别验证javacg2和jacg的Map/List/Set/EL表达式类型参数的修改和查询
 * EL表达式配置的value为String格式（非数组）
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProjectConfigParamTest extends BaseTest {

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
        projectBody.put("description", "项目配置参数专项测试_" + System.currentTimeMillis());

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
     * 项目-javacg2主配置(Map)：修改并验证
     */
    @Test
    @Order(1)
    void testProjectJavaCG2MainConfig() throws Exception {
        ensureProjectCreated();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "项目配置参数专项测试_" + testProjectId);

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, Object> mainConfig = new LinkedHashMap<>();
        mainConfig.put("CKE_PARSE_METHOD_CALL_TYPE_VALUE", false);
        mainConfig.put("CKE_ANALYSE_FIELD_RELATIONSHIP", true);
        mainConfig.put("CKE_LOG_METHOD_SPEND_TIME", false);
        javacg2Config.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), mainConfig);

        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_JAR_DIR", Arrays.asList("/path/to/test.jar"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);

        body.put("javacg2Config", javacg2Config);

        // 更新
        mockMvc.perform(put("/api/v1/projects/{projectId}", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证 - Map配置的value为String格式
        mockMvc.perform(get("/api/v1/projects/{projectId}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_PARSE_METHOD_CALL_TYPE_VALUE").value("false"))
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_ANALYSE_FIELD_RELATIONSHIP").value("true"))
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_LOG_METHOD_SPEND_TIME").value("false"));
    }

    /**
     * 项目-JavaCG2 List配置：修改并验证（顺序保留）
     */
    @Test
    @Order(2)
    void testProjectJavaCG2ListConfig() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "项目配置参数专项测试_" + testProjectId);

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_JAR_DIR", Arrays.asList("/a.jar", "/b.jar", "/c.jar"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);

        body.put("javacg2Config", javacg2Config);

        mockMvc.perform(put("/api/v1/projects/{projectId}", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证 - List配置的value为数组格式，顺序保留
        mockMvc.perform(get("/api/v1/projects/{projectId}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_JAR_DIR[0]").value("/a.jar"))
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_JAR_DIR[1]").value("/b.jar"))
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_JAR_DIR[2]").value("/c.jar"));
    }

    /**
     * 项目-JavaCG2 Set配置：修改并验证（去重）
     */
    @Test
    @Order(3)
    void testProjectJavaCG2SetConfig() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "项目配置参数专项测试_" + testProjectId);

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_JAR_DIR", Arrays.asList("/path/to/test.jar"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);

        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_FR_EQ_CONVERSION_METHOD", Arrays.asList("com.test.X:valueOf=1", "com.test.Y:hashCode=0", "com.test.X:valueOf=1")); // 包含重复
        javacg2Config.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("javacg2Config", javacg2Config);

        mockMvc.perform(put("/api/v1/projects/{projectId}", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证 - Set配置的value为数组格式，去重
        mockMvc.perform(get("/api/v1/projects/{projectId}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.SET_CONFIG.getValue() + ".OCFUSE_FR_EQ_CONVERSION_METHOD").isArray())
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
                    com.fasterxml.jackson.databind.JsonNode setItems = jsonNode.at("/data/javacg2Config/" + ConfigCategoryEnum.SET_CONFIG.getValue() + "/OCFUSE_FR_EQ_CONVERSION_METHOD");
                    // Set去重，不应包含3个元素
                    org.junit.jupiter.api.Assertions.assertTrue(setItems.size() <= 2,
                            "Set配置应去重，实际元素数: " + setItems.size());
                });
    }

    /**
     * 项目-JavaCG2 EL表达式配置：修改并验证
     * EL表达式的value为String格式（非数组）
     */
    @Test
    @Order(4)
    void testProjectJavaCG2ElConfig() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "项目配置参数专项测试_" + testProjectId);

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_JAR_DIR", Arrays.asList("/path/to/test.jar"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);

        Map<String, Object> elConfig = new LinkedHashMap<>();
        elConfig.put("ECE_MERGE_FILE_IGNORE_JAR_IN_DIR", "true");
        javacg2Config.put(ConfigCategoryEnum.EL_CONFIG.getValue(), elConfig);

        body.put("javacg2Config", javacg2Config);

        mockMvc.perform(put("/api/v1/projects/{projectId}", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证 - EL表达式配置的value为String格式，不是数组
        mockMvc.perform(get("/api/v1/projects/{projectId}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.EL_CONFIG.getValue() + ".ECE_MERGE_FILE_IGNORE_JAR_IN_DIR").value("true"))
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
                    com.fasterxml.jackson.databind.JsonNode elValue = jsonNode.at("/data/javacg2Config/" + ConfigCategoryEnum.EL_CONFIG.getValue() + "/ECE_MERGE_FILE_IGNORE_JAR_IN_DIR");
                    // EL表达式value必须是String，不能是数组
                    org.junit.jupiter.api.Assertions.assertFalse(elValue.isArray(),
                            "EL表达式配置的value应为String格式，不应为数组，实际: " + elValue.getNodeType());
                    org.junit.jupiter.api.Assertions.assertEquals("true", elValue.asText(),
                            "EL表达式配置的value应为\"true\"");
                });
    }

    /**
     * 项目-JACG主配置(Map)：修改并验证
     */
    @Test
    @Order(5)
    void testProjectJACGMainConfig() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "项目配置参数专项测试_" + testProjectId);

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_JAR_DIR", Arrays.asList("/path/to/test.jar"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);
        body.put("javacg2Config", javacg2Config);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> mainConfig = new LinkedHashMap<>();
        mainConfig.put("CKE_CALL_GRAPH_OUTPUT_DETAIL", "2");
        mainConfig.put("CKE_THREAD_NUM", "50");
        mainConfig.put("CKE_IGNORE_DUP_CALLEE_IN_ONE_CALLER", true);
        jacgConfig.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), mainConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/projects/{projectId}", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证 - Map配置的value为String格式
        mockMvc.perform(get("/api/v1/projects/{projectId}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_CALL_GRAPH_OUTPUT_DETAIL").value("2"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_THREAD_NUM").value("50"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_IGNORE_DUP_CALLEE_IN_ONE_CALLER").value("true"));
    }

    /**
     * 项目-JACG数据库配置(Map)：修改并验证
     */
    @Test
    @Order(6)
    void testProjectJACGDbConfig() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "项目配置参数专项测试_" + testProjectId);

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        listConfig.put("OCFULE_JAR_DIR", Arrays.asList("/path/to/test.jar"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);
        body.put("javacg2Config", javacg2Config);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> dbConfig = new LinkedHashMap<>();
        dbConfig.put("CDKE_DB_USE_H2", true);
        dbConfig.put("CDKE_DB_H2_FILE_PATH", "./build/custom_h2db");
        jacgConfig.put(ConfigCategoryEnum.DB_CONFIG.getValue(), dbConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/projects/{projectId}", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证 - 数据库配置的value为String格式
        mockMvc.perform(get("/api/v1/projects/{projectId}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.DB_CONFIG.getValue() + ".CDKE_DB_USE_H2").value("true"));
    }

    /**
     * 项目-JACG List配置：修改并验证
     */
    @Test
    @Order(7)
    void testProjectJACGListConfig() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "项目配置参数专项测试_" + testProjectId);

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> javacg2ListConfig = new LinkedHashMap<>();
        javacg2ListConfig.put("OCFULE_JAR_DIR", Arrays.asList("/path/to/test.jar"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), javacg2ListConfig);
        body.put("javacg2Config", javacg2Config);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, List<String>> jacgListConfig = new LinkedHashMap<>();
        jacgListConfig.put("OCFULE_FIND_STACK_KEYWORD_4ER", Arrays.asList("err1", "err2"));
        jacgListConfig.put("OCFULE_FIND_STACK_KEYWORD_4EE", Arrays.asList("ee1"));
        jacgConfig.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), jacgListConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/projects/{projectId}", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证 - List配置的value为数组格式，顺序保留
        mockMvc.perform(get("/api/v1/projects/{projectId}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4ER[0]").value("err1"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4ER[1]").value("err2"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4EE[0]").value("ee1"));
    }

    /**
     * 项目-JACG Set配置：修改并验证
     */
    @Test
    @Order(8)
    void testProjectJACGSetConfig() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "项目配置参数专项测试_" + testProjectId);

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> javacg2ListConfig = new LinkedHashMap<>();
        javacg2ListConfig.put("OCFULE_JAR_DIR", Arrays.asList("/path/to/test.jar"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), javacg2ListConfig);
        body.put("javacg2Config", javacg2Config);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, List<String>> setConfig = new LinkedHashMap<>();
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("C1.m1", "C2.m2"));
        setConfig.put("OCFUSE_METHOD_CLASS_4CALLEE", Arrays.asList("C3.m3"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), setConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/projects/{projectId}", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证 - Set配置的value为数组格式
        mockMvc.perform(get("/api/v1/projects/{projectId}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.SET_CONFIG.getValue() + ".OCFUSE_METHOD_CLASS_4CALLER").isArray())
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.SET_CONFIG.getValue() + ".OCFUSE_METHOD_CLASS_4CALLEE").isArray());
    }

    /**
     * 项目-JACG EL表达式配置：修改并验证
     * EL表达式的value为String格式（非数组）
     */
    @Test
    @Order(9)
    void testProjectJACGElConfig() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "项目配置参数专项测试_" + testProjectId);

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> javacg2ListConfig = new LinkedHashMap<>();
        javacg2ListConfig.put("OCFULE_JAR_DIR", Arrays.asList("/path/to/test.jar"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), javacg2ListConfig);
        body.put("javacg2Config", javacg2Config);

        Map<String, Object> jacgConfig = new LinkedHashMap<>();
        Map<String, Object> elConfig = new LinkedHashMap<>();
        elConfig.put("ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL", "true");
        jacgConfig.put(ConfigCategoryEnum.EL_CONFIG.getValue(), elConfig);

        body.put("jacgConfig", jacgConfig);

        mockMvc.perform(put("/api/v1/projects/{projectId}", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 查询验证 - EL表达式配置的value为String格式，不是数组
        mockMvc.perform(get("/api/v1/projects/{projectId}", testProjectId))
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
