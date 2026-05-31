package com.github.adrninistrator.jacgserver.controller;

import com.github.adrninistrator.jacgserver.BaseTest;
import com.github.adrninistrator.jacgserver.constant.ConfigCategoryEnum;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.Collections;
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
 * 项目控制器测试
 * 包含项目CRUD、配置参数保存与验证
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProjectControllerTest extends BaseTest {

    private static String testProjectId;
    private static String copiedProjectId;

    /**
     * 构建创建项目的请求体（包含完整的javacg2和jacg配置）
     */
    private String buildCreateProjectBody(String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", description);

        // javacg2配置
        Map<String, Object> javacg2Config = new LinkedHashMap<>();

        Map<String, Object> javacg2MainConfig = new LinkedHashMap<>();
        javacg2MainConfig.put("CKE_PARSE_METHOD_CALL_TYPE_VALUE", true);
        javacg2Config.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), javacg2MainConfig);

        Map<String, List<String>> javacg2ListConfig = new LinkedHashMap<>();
        javacg2ListConfig.put("OCFULE_JAR_DIR", Arrays.asList("/path/to/jar1"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), javacg2ListConfig);

        Map<String, List<String>> javacg2SetConfig = new LinkedHashMap<>();
        javacg2SetConfig.put("OCFUSE_FR_EQ_CONVERSION_METHOD", Arrays.asList("com.test.Class1:valueOf=1"));
        javacg2Config.put(ConfigCategoryEnum.SET_CONFIG.getValue(), javacg2SetConfig);

        Map<String, Object> javacg2ElConfig = new LinkedHashMap<>();
        javacg2ElConfig.put("ECE_MERGE_FILE_IGNORE_JAR_IN_DIR", "true");
        javacg2Config.put(ConfigCategoryEnum.EL_CONFIG.getValue(), javacg2ElConfig);

        body.put("javacg2Config", javacg2Config);

        // jacg配置
        Map<String, Object> jacgConfig = new LinkedHashMap<>();

        Map<String, Object> jacgMainConfig = new LinkedHashMap<>();
        jacgMainConfig.put("CKE_CALL_GRAPH_OUTPUT_DETAIL", "1");
        jacgMainConfig.put("CKE_THREAD_NUM", "10");
        jacgConfig.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), jacgMainConfig);

        Map<String, Object> jacgDbConfig = new LinkedHashMap<>();
        jacgDbConfig.put("CDKE_DB_USE_H2", true);
        jacgConfig.put(ConfigCategoryEnum.DB_CONFIG.getValue(), jacgDbConfig);

        Map<String, List<String>> jacgListConfig = new LinkedHashMap<>();
        jacgListConfig.put("OCFULE_FIND_STACK_KEYWORD_4ER", Arrays.asList("keyword1"));
        jacgConfig.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), jacgListConfig);

        Map<String, List<String>> jacgSetConfig = new LinkedHashMap<>();
        jacgSetConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("Class1.method1"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), jacgSetConfig);

        Map<String, Object> jacgElConfig = new LinkedHashMap<>();
        jacgElConfig.put("ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL", "false");
        jacgConfig.put(ConfigCategoryEnum.EL_CONFIG.getValue(), jacgElConfig);

        body.put("jacgConfig", jacgConfig);

        return toJson(body);
    }

    /**
     * 构建更新项目的请求体（修改各类型配置参数）
     */
    private String buildUpdateProjectBody(String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", description);

        // javacg2配置 - 修改后的值
        Map<String, Object> javacg2Config = new LinkedHashMap<>();

        Map<String, Object> javacg2MainConfig = new LinkedHashMap<>();
        javacg2MainConfig.put("CKE_PARSE_METHOD_CALL_TYPE_VALUE", false);
        javacg2MainConfig.put("CKE_ANALYSE_FIELD_RELATIONSHIP", true);
        javacg2Config.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), javacg2MainConfig);

        Map<String, List<String>> javacg2ListConfig = new LinkedHashMap<>();
        javacg2ListConfig.put("OCFULE_JAR_DIR", Arrays.asList("/path/to/jar2", "/path/to/jar3"));
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), javacg2ListConfig);

        Map<String, List<String>> javacg2SetConfig = new LinkedHashMap<>();
        javacg2SetConfig.put("OCFUSE_FR_EQ_CONVERSION_METHOD", Arrays.asList("com.test.Class2:valueOf=1", "com.test.Class3:hashCode=0"));
        javacg2Config.put(ConfigCategoryEnum.SET_CONFIG.getValue(), javacg2SetConfig);

        Map<String, Object> javacg2ElConfig = new LinkedHashMap<>();
        javacg2ElConfig.put("ECE_MERGE_FILE_IGNORE_JAR_IN_DIR", "false");
        javacg2Config.put(ConfigCategoryEnum.EL_CONFIG.getValue(), javacg2ElConfig);

        body.put("javacg2Config", javacg2Config);

        // jacg配置 - 修改后的值
        Map<String, Object> jacgConfig = new LinkedHashMap<>();

        Map<String, Object> jacgMainConfig = new LinkedHashMap<>();
        jacgMainConfig.put("CKE_CALL_GRAPH_OUTPUT_DETAIL", "2");
        jacgMainConfig.put("CKE_THREAD_NUM", "30");
        jacgConfig.put(ConfigCategoryEnum.MAIN_CONFIG.getValue(), jacgMainConfig);

        Map<String, Object> jacgDbConfig = new LinkedHashMap<>();
        jacgDbConfig.put("CDKE_DB_USE_H2", true);
        jacgConfig.put(ConfigCategoryEnum.DB_CONFIG.getValue(), jacgDbConfig);

        Map<String, List<String>> jacgListConfig = new LinkedHashMap<>();
        jacgListConfig.put("OCFULE_FIND_STACK_KEYWORD_4ER", Arrays.asList("keyword2", "keyword3"));
        jacgConfig.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), jacgListConfig);

        Map<String, List<String>> jacgSetConfig = new LinkedHashMap<>();
        jacgSetConfig.put("OCFUSE_METHOD_CLASS_4CALLER", Arrays.asList("Class2.method2", "Class3.method3"));
        jacgConfig.put(ConfigCategoryEnum.SET_CONFIG.getValue(), jacgSetConfig);

        Map<String, Object> jacgElConfig = new LinkedHashMap<>();
        jacgElConfig.put("ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL", "true");
        jacgConfig.put(ConfigCategoryEnum.EL_CONFIG.getValue(), jacgElConfig);

        body.put("jacgConfig", jacgConfig);

        return toJson(body);
    }

    @Test
    @Order(1)
    void testCreateProjectWithFullConfig() throws Exception {
        String body = buildCreateProjectBody("测试项目_配置参数");
        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectId").exists())
                .andExpect(jsonPath("$.data.description").value("测试项目_配置参数"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        testProjectId = extractJsonValue(response, "$.data.projectId");
    }

    @Test
    @Order(2)
    void testGetProjectVerifyConfig() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{projectId}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // javacg2 mainConfig
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_PARSE_METHOD_CALL_TYPE_VALUE").value("true"))
                // javacg2 listConfig
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_JAR_DIR").isArray())
                // javacg2 setConfig
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.SET_CONFIG.getValue() + ".OCFUSE_FR_EQ_CONVERSION_METHOD").isArray())
                // javacg2 elConfig
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.EL_CONFIG.getValue() + ".ECE_MERGE_FILE_IGNORE_JAR_IN_DIR").value("true"))
                // jacg mainConfig
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_CALL_GRAPH_OUTPUT_DETAIL").value("1"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_THREAD_NUM").value("10"))
                // JACG dbConfig
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.DB_CONFIG.getValue() + ".CDKE_DB_USE_H2").value("true"))
                // JACG listConfig
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4ER").isArray())
                // JACG setConfig
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.SET_CONFIG.getValue() + ".OCFUSE_METHOD_CLASS_4CALLER").isArray())
                // JACG elConfig
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.EL_CONFIG.getValue() + ".ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL").value("false"));
    }

    @Test
    @Order(3)
    void testUpdateProjectConfig() throws Exception {
        String body = buildUpdateProjectBody("测试项目_配置参数");
        mockMvc.perform(put("/api/v1/projects/{projectId}", testProjectId)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(4)
    void testGetProjectVerifyUpdatedConfig() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{projectId}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // javacg2 mainConfig - 修改后的值
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_PARSE_METHOD_CALL_TYPE_VALUE").value("false"))
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_ANALYSE_FIELD_RELATIONSHIP").value("true"))
                // javacg2 listConfig - 修改后的值
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_JAR_DIR[0]").value("/path/to/jar2"))
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_JAR_DIR[1]").value("/path/to/jar3"))
                // javacg2 setConfig
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.SET_CONFIG.getValue() + ".OCFUSE_FR_EQ_CONVERSION_METHOD").isArray())
                // javacg2 elConfig
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.EL_CONFIG.getValue() + ".ECE_MERGE_FILE_IGNORE_JAR_IN_DIR").value("false"))
                // jacg mainConfig - 修改后的值
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_CALL_GRAPH_OUTPUT_DETAIL").value("2"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_THREAD_NUM").value("30"))
                // JACG dbConfig
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.DB_CONFIG.getValue() + ".CDKE_DB_USE_H2").value("true"))
                // JACG listConfig
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4ER[0]").value("keyword2"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.LIST_CONFIG.getValue() + ".OCFULE_FIND_STACK_KEYWORD_4ER[1]").value("keyword3"))
                // JACG setConfig
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.SET_CONFIG.getValue() + ".OCFUSE_METHOD_CLASS_4CALLER").isArray())
                // JACG elConfig
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.EL_CONFIG.getValue() + ".ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL").value("true"));
    }

    @Test
    @Order(5)
    void testListProjects() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projects").isArray());
    }

    @Test
    @Order(6)
    void testGetNonExistentProject() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{projectId}", "nonexistent_project_id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @Order(7)
    void testCreateProjectEmptyDescription() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "");
        mockMvc.perform(post("/api/v1/projects")
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    @Order(8)
    void testCreateProjectDuplicateDescription() throws Exception {
        String body = buildCreateProjectBody("测试项目_配置参数");
        mockMvc.perform(post("/api/v1/projects")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    @Order(9)
    void testCreateProjectNoJarPaths() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "测试项目_无Jar_" + System.currentTimeMillis());

        Map<String, Object> javacg2Config = new LinkedHashMap<>();
        Map<String, List<String>> listConfig = new LinkedHashMap<>();
        // 不设置 OCFULE_JAR_DIR
        javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);
        body.put("javacg2Config", javacg2Config);

        mockMvc.perform(post("/api/v1/projects")
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    @Order(10)
    void testCopyProject() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "复制的测试项目");

        MvcResult result = mockMvc.perform(post("/api/v1/projects/{projectId}/copy", testProjectId)
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectId").exists())
                .andExpect(jsonPath("$.data.javacg2Config").exists())
                .andExpect(jsonPath("$.data.jacgConfig").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        copiedProjectId = extractJsonValue(response, "$.data.projectId");
    }

    @Test
    @Order(11)
    void testVerifyCopiedProjectConfig() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{projectId}", copiedProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.javacg2Config." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_PARSE_METHOD_CALL_TYPE_VALUE").value("false"))
                .andExpect(jsonPath("$.data.jacgConfig." + ConfigCategoryEnum.MAIN_CONFIG.getValue() + ".CKE_CALL_GRAPH_OUTPUT_DETAIL").value("2"));
    }

    @Test
    @Order(12)
    void testBatchDeleteProjects() throws Exception {
        // 先创建2个项目用于批量删除
        String[] projectIds = new String[2];
        for (int i = 0; i < 2; i++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("description", "批量删除测试项目_" + i + "_" + System.currentTimeMillis());
            Map<String, Object> javacg2Config = new LinkedHashMap<>();
            Map<String, List<String>> listConfig = new LinkedHashMap<>();
            listConfig.put("OCFULE_JAR_DIR", Arrays.asList("/tmp/fake.jar"));
            javacg2Config.put(ConfigCategoryEnum.LIST_CONFIG.getValue(), listConfig);
            body.put("javacg2Config", javacg2Config);

            MvcResult result = mockMvc.perform(post("/api/v1/projects")
                            .contentType("application/json")
                            .content(toJson(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andReturn();
            projectIds[i] = extractJsonValue(result.getResponse().getContentAsString(), "$.data.projectId");
            waitForIdGeneration();
        }

        // 批量删除
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectIds", Arrays.asList(projectIds));
        mockMvc.perform(post("/api/v1/projects/batch-delete")
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.successCount").exists());
    }

    @Test
    @Order(13)
    void testBatchDeleteEmptyList() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("projectIds", Collections.emptyList());
        mockMvc.perform(post("/api/v1/projects/batch-delete")
                        .contentType("application/json")
                        .content(toJson(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(90)
    void testDeleteCopiedProject() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/{projectId}", copiedProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(91)
    void testDeleteOriginalProject() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/{projectId}", testProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
