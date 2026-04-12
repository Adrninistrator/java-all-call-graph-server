package com.github.adrninistrator.jacgserver.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.adrninistrator.jacgserver.BaseTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * 项目管理控制器测试
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProjectControllerTest extends BaseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 测试创建项目
     */
    @Test
    @Order(1)
    void testCreateProject() throws Exception {
        String requestBody = "{" +
                "\"description\": \"测试项目\"," +
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
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectId").exists())
                .andExpect(jsonPath("$.data.description").value("测试项目"))
                .andReturn();

        // 提取项目ID
        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        createdProjectId = jsonNode.path("data").path("projectId").asText();

        assertNotNull(createdProjectId, "项目ID不应为空");
        assertTrue(createdProjectId.length() == 17, "项目ID应为17位时间戳");

        // 验证项目目录创建
        Path projectDir = Paths.get(configService.getProjectConfDir(), createdProjectId);
        assertTrue(Files.exists(projectDir), "项目目录应该存在");

        // 验证子目录创建
        assertTrue(Files.exists(projectDir.resolve("templates")), "templates目录应该存在");

        System.out.println("创建项目成功: " + createdProjectId);
    }

    /**
     * 测试获取项目列表
     */
    @Test
    @Order(2)
    void testListProjects() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projects").isArray())
                .andExpect(jsonPath("$.data.projects[0].projectId").value(createdProjectId));
    }

    /**
     * 测试获取项目详情
     */
    @Test
    @Order(3)
    void testGetProject() throws Exception {
        mockMvc.perform(get("/api/v1/projects/" + createdProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectId").value(createdProjectId))
                .andExpect(jsonPath("$.data.javaCG2Config").exists())
                .andExpect(jsonPath("$.data.jacgConfig").exists());
    }

    /**
     * 测试更新项目
     */
    @Test
    @Order(4)
    void testUpdateProject() throws Exception {
        String requestBody = "{" +
                "\"description\": \"更新后的测试项目\"," +
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

        mockMvc.perform(put("/api/v1/projects/" + createdProjectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证更新后的数据
        mockMvc.perform(get("/api/v1/projects/" + createdProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("更新后的测试项目"));
    }

    /**
     * 测试复制项目
     */
    @Test
    @Order(5)
    void testCopyProject() throws Exception {
        waitForIdGeneration();

        String requestBody = "{\"description\": \"复制的测试项目\"}";

        MvcResult result = mockMvc.perform(post("/api/v1/projects/" + createdProjectId + "/copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectId").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        copiedProjectId = jsonNode.path("data").path("projectId").asText();

        assertNotNull(copiedProjectId, "复制的项目ID不应为空");
        assertNotEquals(createdProjectId, copiedProjectId, "复制的项目ID应该与原项目ID不同");

        // 验证复制后的项目信息
        mockMvc.perform(get("/api/v1/projects/" + copiedProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("复制的测试项目"));
    }

    /**
     * 测试获取不存在的项目
     */
    @Test
    @Order(6)
    void testGetNonExistentProject() throws Exception {
        mockMvc.perform(get("/api/v1/projects/nonexistent_project_id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    /**
     * 测试删除复制的项目
     */
    @Test
    @Order(90)
    void testDeleteCopiedProject() throws Exception {
        if (copiedProjectId == null) {
            return;
        }

        mockMvc.perform(delete("/api/v1/projects/" + copiedProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证项目目录已删除
        Path projectDir = Paths.get(configService.getProjectConfDir(), copiedProjectId);
        assertFalse(Files.exists(projectDir), "复制的项目目录应该已被删除");
    }

    /**
     * 测试删除项目
     */
    @Test
    @Order(91)
    void testDeleteProject() throws Exception {
        if (createdProjectId == null) {
            return;
        }

        mockMvc.perform(delete("/api/v1/projects/" + createdProjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证项目目录已删除
        Path projectDir = Paths.get(configService.getProjectConfDir(), createdProjectId);
        assertFalse(Files.exists(projectDir), "项目目录应该已被删除");
    }
}
