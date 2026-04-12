package com.github.adrninistrator.jacgserver.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.adrninistrator.jacgserver.BaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 模板管理控制器测试
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TemplateControllerTest extends BaseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String testProjectId;

    @BeforeAll
    void setupProject() throws Exception {
        // 创建一个测试用的项目
        String requestBody = "{" +
                "\"description\": \"模板测试项目\"," +
                "\"javaCG2Config\": {" +
                "\"mainConfig\": {}," +
                "\"listConfig\": {}," +
                "\"setConfig\": {}," +
                "\"elConfig\": {}" +
                "}," +
                "\"jacgConfig\": {" +
                "\"mainConfig\": {}," +
                "\"dbConfig\": {}," +
                "\"listConfig\": {}," +
                "\"setConfig\": {}," +
                "\"elConfig\": {}" +
                "}" +
                "}";

        MvcResult result = mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        testProjectId = jsonNode.path("data").path("projectId").asText();

        System.out.println("创建测试项目成功: " + testProjectId);
    }

    @AfterAll
    void cleanupProject() throws Exception {
        // 清理测试用的项目
        if (testProjectId != null) {
            mockMvc.perform(delete("/api/v1/projects/" + testProjectId))
                    .andExpect(status().isOk());
            System.out.println("删除测试项目成功: " + testProjectId);
        }
    }

    /**
     * 测试创建模板
     */
    @Test
    @Order(1)
    void testCreateTemplate() throws Exception {
        String requestBody = "{" +
                "\"description\": \"测试模板\"," +
                "\"direction\": \"caller\"," +
                "\"jacgConfig\": {" +
                "\"mainConfig\": {}," +
                "\"dbConfig\": {}," +
                "\"listConfig\": {}," +
                "\"setConfig\": {}," +
                "\"elConfig\": {}" +
                "}" +
                "}";

        MvcResult result = mockMvc.perform(post("/api/v1/projects/" + testProjectId + "/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templateId").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        createdTemplateId = jsonNode.path("data").path("templateId").asText();

        assertNotNull(createdTemplateId, "模板ID不应为空");
        assertTrue(createdTemplateId.length() == 17, "模板ID应为17位时间戳");

        System.out.println("创建模板成功: " + createdTemplateId);
    }

    /**
     * 测试获取模板列表
     */
    @Test
    @Order(2)
    void testListTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/projects/" + testProjectId + "/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templates").isArray())
                .andExpect(jsonPath("$.data.templates[0].templateId").value(createdTemplateId));
    }

    /**
     * 测试获取模板详情
     */
    @Test
    @Order(3)
    void testGetTemplate() throws Exception {
        mockMvc.perform(get("/api/v1/templates/" + createdTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templateId").value(createdTemplateId))
                .andExpect(jsonPath("$.data.direction").value("caller"));
    }

    /**
     * 测试更新模板
     */
    @Test
    @Order(4)
    void testUpdateTemplate() throws Exception {
        String requestBody = "{" +
                "\"description\": \"更新后的测试模板\"," +
                "\"direction\": \"callee\"," +
                "\"jacgConfig\": {" +
                "\"mainConfig\": {}," +
                "\"dbConfig\": {}," +
                "\"listConfig\": {}," +
                "\"setConfig\": {}," +
                "\"elConfig\": {}" +
                "}" +
                "}";

        mockMvc.perform(put("/api/v1/templates/" + createdTemplateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证更新后的数据
        mockMvc.perform(get("/api/v1/templates/" + createdTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("更新后的测试模板"))
                .andExpect(jsonPath("$.data.direction").value("callee"));
    }

    /**
     * 测试复制模板
     */
    @Test
    @Order(5)
    void testCopyTemplate() throws Exception {
        waitForIdGeneration();

        String requestBody = "{" +
                "\"description\": \"复制的测试模板\"" +
                "}";

        MvcResult result = mockMvc.perform(post("/api/v1/templates/" + createdTemplateId + "/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.templateId").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        copiedTemplateId = jsonNode.path("data").path("templateId").asText();

        assertNotNull(copiedTemplateId, "复制的模板ID不应为空");
        assertNotEquals(createdTemplateId, copiedTemplateId, "复制的模板ID应该与原模板ID不同");

        // 验证复制后的模板信息
        mockMvc.perform(get("/api/v1/templates/" + copiedTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("复制的测试模板"));
    }

    /**
     * 测试获取不存在的模板
     */
    @Test
    @Order(6)
    void testGetNonExistentTemplate() throws Exception {
        mockMvc.perform(get("/api/v1/templates/nonexistent_template_id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }

    /**
     * 测试创建模板时项目不存在
     */
    @Test
    @Order(7)
    void testCreateTemplateWithNonExistentProject() throws Exception {
        String requestBody = "{" +
                "\"description\": \"测试模板\"," +
                "\"direction\": \"caller\"" +
                "}";

        mockMvc.perform(post("/api/v1/projects/nonexistent_project/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    /**
     * 测试删除复制的模板
     */
    @Test
    @Order(90)
    void testDeleteCopiedTemplate() throws Exception {
        if (copiedTemplateId == null) {
            return;
        }

        mockMvc.perform(delete("/api/v1/templates/" + copiedTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 测试删除模板
     */
    @Test
    @Order(91)
    void testDeleteTemplate() throws Exception {
        if (createdTemplateId == null) {
            return;
        }

        mockMvc.perform(delete("/api/v1/templates/" + createdTemplateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
