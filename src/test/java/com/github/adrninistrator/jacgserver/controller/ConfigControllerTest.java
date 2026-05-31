package com.github.adrninistrator.jacgserver.controller;

import com.github.adrninistrator.jacgserver.BaseTest;
import com.github.adrninistrator.jacgserver.constant.ConfigCategoryEnum;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 配置参数控制器测试
 * 覆盖配置定义、EL菜单、配置描述等接口
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConfigControllerTest extends BaseTest {

    @Test
    @Order(1)
    void testGetConfigDefinitionsDefault() throws Exception {
        mockMvc.perform(get("/api/v1/config/definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.javacg2").exists())
                .andExpect(jsonPath("$.data.jacg").exists())
                .andExpect(jsonPath("$.data.javacg2." + ConfigCategoryEnum.MAIN_CONFIG.getValue()).isArray())
                .andExpect(jsonPath("$.data.javacg2." + ConfigCategoryEnum.LIST_CONFIG.getValue()).isArray())
                .andExpect(jsonPath("$.data.javacg2." + ConfigCategoryEnum.SET_CONFIG.getValue()).isArray())
                .andExpect(jsonPath("$.data.javacg2." + ConfigCategoryEnum.EL_CONFIG.getValue()).isArray())
                .andExpect(jsonPath("$.data.jacg." + ConfigCategoryEnum.MAIN_CONFIG.getValue()).isArray())
                .andExpect(jsonPath("$.data.jacg." + ConfigCategoryEnum.DB_CONFIG.getValue()).isArray())
                .andExpect(jsonPath("$.data.jacg." + ConfigCategoryEnum.LIST_CONFIG.getValue()).isArray())
                .andExpect(jsonPath("$.data.jacg." + ConfigCategoryEnum.SET_CONFIG.getValue()).isArray())
                .andExpect(jsonPath("$.data.jacg." + ConfigCategoryEnum.EL_CONFIG.getValue()).isArray());
    }

    @Test
    @Order(2)
    void testGetConfigDefinitionsProject() throws Exception {
        mockMvc.perform(get("/api/v1/config/definitions/project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.javacg2").exists())
                .andExpect(jsonPath("$.data.jacg").exists())
                .andExpect(jsonPath("$.data.javacg2." + ConfigCategoryEnum.MAIN_CONFIG.getValue()).isArray())
                .andExpect(jsonPath("$.data.jacg." + ConfigCategoryEnum.MAIN_CONFIG.getValue()).isArray())
                .andExpect(result -> {
                    // 验证项目场景下OCFUSE_METHOD_CLASS_4CALLEE/4CALLER的editable不为false
                    String response = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
                    com.fasterxml.jackson.databind.JsonNode setConfig = jsonNode.at("/data/jacg/" + ConfigCategoryEnum.SET_CONFIG.getValue());
                    if (setConfig.isArray()) {
                        for (int i = 0; i < setConfig.size(); i++) {
                            com.fasterxml.jackson.databind.JsonNode item = setConfig.get(i);
                            String key = item.path("key").asText();
                            if ("OCFUSE_METHOD_CLASS_4CALLEE".equals(key) || "OCFUSE_METHOD_CLASS_4CALLER".equals(key)) {
                                // 项目场景下editable不应为false
                                boolean editable = item.path("editable").asBoolean(true);
                                org.junit.jupiter.api.Assertions.assertTrue(editable,
                                        key + "在项目场景下editable应为true（默认值），实际为false");
                            }
                        }
                    }
                });
    }

    @Test
    @Order(3)
    void testGetConfigDefinitionsTemplate() throws Exception {
        mockMvc.perform(get("/api/v1/config/definitions/template"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jacg").exists())
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
                    com.fasterxml.jackson.databind.JsonNode setConfig = jsonNode.at("/data/jacg/" + ConfigCategoryEnum.SET_CONFIG.getValue());
                    org.junit.jupiter.api.Assertions.assertTrue(setConfig.isArray(), "setConfig应为数组");

                    // 验证模板场景下OCFUSE_METHOD_CLASS_4CALLEE和OCFUSE_METHOD_CLASS_4CALLER的editable=false
                    boolean foundCallee = false;
                    boolean foundCaller = false;
                    for (int i = 0; i < setConfig.size(); i++) {
                        com.fasterxml.jackson.databind.JsonNode item = setConfig.get(i);
                        String key = item.path("key").asText();
                        if ("OCFUSE_METHOD_CLASS_4CALLEE".equals(key)) {
                            foundCallee = true;
                            org.junit.jupiter.api.Assertions.assertFalse(item.path("editable").asBoolean(true),
                                    "OCFUSE_METHOD_CLASS_4CALLEE在模板场景下editable应为false");
                        }
                        if ("OCFUSE_METHOD_CLASS_4CALLER".equals(key)) {
                            foundCaller = true;
                            org.junit.jupiter.api.Assertions.assertFalse(item.path("editable").asBoolean(true),
                                    "OCFUSE_METHOD_CLASS_4CALLER在模板场景下editable应为false");
                        }
                    }
                    org.junit.jupiter.api.Assertions.assertTrue(foundCallee, "应找到OCFUSE_METHOD_CLASS_4CALLEE配置项");
                    org.junit.jupiter.api.Assertions.assertTrue(foundCaller, "应找到OCFUSE_METHOD_CLASS_4CALLER配置项");
                });
    }

    @Test
    @Order(4)
    void testGetConfigDefinitionsInvalidScene() throws Exception {
        mockMvc.perform(get("/api/v1/config/definitions/invalid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @Order(5)
    void testGetElFixedMenu() throws Exception {
        mockMvc.perform(get("/api/v1/config/el-menu/fixed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.categories").isArray());
    }

    @Test
    @Order(6)
    void testGetJACGElVariableMenu() throws Exception {
        mockMvc.perform(get("/api/v1/config/el-menu/variables/jacg/ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.variables").isArray());
    }

    @Test
    @Order(7)
    void testGetJavaCG2ElVariableMenu() throws Exception {
        mockMvc.perform(get("/api/v1/config/el-menu/variables/javacg2/ECE_MERGE_FILE_IGNORE_JAR_IN_DIR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.variables").isArray());
    }

    @Test
    @Order(8)
    void testGetJACGElExampleContent() throws Exception {
        mockMvc.perform(get("/api/v1/config/el-example/jacg/ECE_GEN_ALL_CALL_GRAPH_IGNORE_METHOD_CALL"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
                    int code = jsonNode.path("code").asInt();
                    org.junit.jupiter.api.Assertions.assertTrue(code == 200 || code == 404,
                            "期望返回200或404，实际: " + code);
                });
    }

    @Test
    @Order(9)
    void testGetJavaCG2ElExampleContent() throws Exception {
        mockMvc.perform(get("/api/v1/config/el-example/javacg2/ECE_MERGE_FILE_IGNORE_JAR_IN_DIR"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
                    int code = jsonNode.path("code").asInt();
                    org.junit.jupiter.api.Assertions.assertTrue(code == 200 || code == 404,
                            "期望返回200或404，实际: " + code);
                });
    }

    @Test
    @Order(10)
    void testGetJACGListConfigDescription() throws Exception {
        mockMvc.perform(get("/api/v1/config/description/jacg-list/OCFULE_FIND_STACK_KEYWORD_4ER"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
                    int code = jsonNode.path("code").asInt();
                    org.junit.jupiter.api.Assertions.assertTrue(code == 200 || code == 404,
                            "期望返回200或404，实际: " + code);
                });
    }

    @Test
    @Order(11)
    void testGetJACGSetConfigDescription() throws Exception {
        mockMvc.perform(get("/api/v1/config/description/jacg-set/OCFUSE_METHOD_CLASS_4CALLER"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
                    int code = jsonNode.path("code").asInt();
                    org.junit.jupiter.api.Assertions.assertTrue(code == 200 || code == 404,
                            "期望返回200或404，实际: " + code);
                });
    }

    @Test
    @Order(12)
    void testGetJavaCG2ListConfigDescription() throws Exception {
        mockMvc.perform(get("/api/v1/config/description/javacg2-list/OCFULE_JAR_DIR"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
                    int code = jsonNode.path("code").asInt();
                    org.junit.jupiter.api.Assertions.assertTrue(code == 200 || code == 404,
                            "期望返回200或404，实际: " + code);
                });
    }

    @Test
    @Order(13)
    void testGetJavaCG2SetConfigDescription() throws Exception {
        mockMvc.perform(get("/api/v1/config/description/javacg2-set/OCFUSE_FR_EQ_CONVERSION_METHOD"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
                    int code = jsonNode.path("code").asInt();
                    org.junit.jupiter.api.Assertions.assertTrue(code == 200 || code == 404,
                            "期望返回200或404，实际: " + code);
                });
    }

    @Test
    @Order(14)
    void testGetEntryPointDescriptionCaller() throws Exception {
        mockMvc.perform(get("/api/v1/config/description/entry-point/caller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(15)
    void testGetEntryPointDescriptionCallee() throws Exception {
        mockMvc.perform(get("/api/v1/config/description/entry-point/callee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(16)
    void testGetFindStackKeywordDescriptionCaller() throws Exception {
        mockMvc.perform(get("/api/v1/config/description/find-stack-keyword/caller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @Order(17)
    void testGetFindStackKeywordDescriptionCallee() throws Exception {
        mockMvc.perform(get("/api/v1/config/description/find-stack-keyword/callee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
