package com.github.adrninistrator.jacgserver;

import com.github.adrninistrator.jacgserver.service.ConfigService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * 测试基类
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ConfigService configService;

    private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 标记项目配置目录是否已清理过（整个JVM生命周期只清理一次，避免影响同一测试类内的有序测试）
     */
    private static boolean projectConfCleaned = false;

    @BeforeEach
    void setUp() throws Exception {
        String outputRootPath = configService.getOutputRootPath();
        Path outputPath = Paths.get(outputRootPath);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }

        // 仅在首次运行时清理项目配置目录，避免残留数据影响测试
        // 不在每次测试方法前都清理，否则同一测试类中有序测试之间的数据会被清除
        if (!projectConfCleaned) {
            String projectConfDir = configService.getProjectConfDir();
            Path projectConfPath = Paths.get(projectConfDir);
            if (Files.exists(projectConfPath)) {
                deleteDirectory(projectConfPath.toFile());
            }
            projectConfCleaned = true;
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    @AfterEach
    void tearDown() throws Exception {
        // 可根据需要启用清理
    }

    /**
     * 将对象转为JSON字符串
     */
    protected String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("对象转JSON失败", e);
        }
    }

    /**
     * 从JSON响应中提取指定路径的值
     */
    protected String extractJsonValue(String json, String jsonPath) {
        return JsonPath.read(json, jsonPath);
    }

    /**
     * 等待1毫秒，确保ID不冲突
     */
    protected void waitForIdGeneration() throws InterruptedException {
        Thread.sleep(1);
    }

    /**
     * 获取当前项目编译后的class目录路径，用于静态分析
     *
     * @return class目录的绝对路径
     */
    protected String getClassDirPath() {
        String projectDir = System.getProperty("user.dir");
        return projectDir + File.separator + "build" + File.separator + "classes" + File.separator + "java" + File.separator + "main";
    }

    /**
     * 等待异步执行完成，轮询执行状态
     *
     * @param execId 执行ID
     * @param maxWaitMs 最大等待时间（毫秒）
     * @return 最终状态（completed/failed），超时返回null
     */
    protected String waitForExecutionComplete(String execId, long maxWaitMs) throws Exception {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            String responseBody = mockMvc.perform(get("/api/v1/executions/{execId}/status", execId))
                    .andReturn().getResponse().getContentAsString();
            com.fasterxml.jackson.databind.JsonNode jsonNode = OBJECT_MAPPER.readTree(responseBody);
            int code = jsonNode.path("code").asInt();
            if (code != 200) {
                return null;
            }
            String status = jsonNode.path("data").path("status").asText();
            if ("completed".equals(status) || "failed".equals(status)) {
                return status;
            }
            Thread.sleep(500);
        }
        return null;
    }
}
