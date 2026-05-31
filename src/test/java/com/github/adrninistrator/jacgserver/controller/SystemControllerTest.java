package com.github.adrninistrator.jacgserver.controller;

import com.github.adrninistrator.jacgserver.BaseTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 系统控制器测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SystemControllerTest extends BaseTest {

    /**
     * 测试获取输出根目录
     */
    @Test
    @Order(1)
    void testGetOutputRootPath() throws Exception {
        mockMvc.perform(get("/api/v1/system/output-root-path"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.outputRootPath").exists())
                .andExpect(result -> {
                    String path = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readTree(result.getResponse().getContentAsString())
                            .path("data").path("outputRootPath").asText();
                    org.junit.jupiter.api.Assertions.assertFalse(path.isEmpty(),
                            "输出根目录不应为空");
                });
    }

    /**
     * 测试健康检查
     */
    @Test
    @Order(2)
    void testHealth() throws Exception {
        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.timestamp").exists());
    }

    /**
     * 测试获取应用根目录
     */
    @Test
    @Order(3)
    void testGetAppRootPath() throws Exception {
        mockMvc.perform(get("/api/v1/system/app-root-path"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.appRootPath").exists())
                .andExpect(result -> {
                    String path = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readTree(result.getResponse().getContentAsString())
                            .path("data").path("appRootPath").asText();
                    org.junit.jupiter.api.Assertions.assertFalse(path.isEmpty(),
                            "应用根目录不应为空");
                });
    }

    /**
     * 测试打开项目日志目录（项目ID为空）
     */
    @Test
    @Order(4)
    void testOpenProjectLogDirectoryEmptyProjectId() throws Exception {
        String requestBody = "{\"projectId\": \"\"}";
        mockMvc.perform(post("/api/v1/system/open-project-log-directory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 测试打开项目日志目录（项目不存在）
     */
    @Test
    @Order(5)
    void testOpenProjectLogDirectoryNotFound() throws Exception {
        String requestBody = "{\"projectId\": \"nonexistent_project_id\"}";
        mockMvc.perform(post("/api/v1/system/open-project-log-directory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode =
                            new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
                    int code = jsonNode.path("code").asInt();
                    // 项目不存在返回1001，或打开目录失败返回500
                    org.junit.jupiter.api.Assertions.assertTrue(
                            code == 1001 || code == 500,
                            "期望返回1001(项目不存在)或500(打开目录失败)，实际: " + code);
                });
    }

    /**
     * 测试打开执行记录输出目录（记录类型为空）
     */
    @Test
    @Order(6)
    void testOpenExecutionOutputDirectoryEmptyRecordType() throws Exception {
        String requestBody = "{\"recordType\": \"\", \"recordId\": 1}";
        mockMvc.perform(post("/api/v1/system/open-execution-output-directory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 测试打开执行记录输出目录（记录ID为空）
     */
    @Test
    @Order(7)
    void testOpenExecutionOutputDirectoryNullRecordId() throws Exception {
        String requestBody = "{\"recordType\": \"analysis\"}";
        mockMvc.perform(post("/api/v1/system/open-execution-output-directory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 测试打开执行记录输出目录（记录ID格式不正确）
     */
    @Test
    @Order(8)
    void testOpenExecutionOutputDirectoryInvalidRecordId() throws Exception {
        String requestBody = "{\"recordType\": \"analysis\", \"recordId\": \"abc\"}";
        mockMvc.perform(post("/api/v1/system/open-execution-output-directory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 测试打开执行记录输出目录（记录不存在）
     */
    @Test
    @Order(9)
    void testOpenExecutionOutputDirectoryRecordNotFound() throws Exception {
        String requestBody = "{\"recordType\": \"analysis\", \"recordId\": 999999}";
        mockMvc.perform(post("/api/v1/system/open-execution-output-directory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode =
                            new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
                    int code = jsonNode.path("code").asInt();
                    // 记录不存在返回404，或打开目录失败返回500
                    org.junit.jupiter.api.Assertions.assertTrue(
                            code == 404 || code == 500,
                            "期望返回404(记录不存在)或500(打开目录失败)，实际: " + code);
                });
    }
}
