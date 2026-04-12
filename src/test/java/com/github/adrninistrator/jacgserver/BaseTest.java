package com.github.adrninistrator.jacgserver;

import com.github.adrninistrator.jacgserver.service.ConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

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

    protected static String createdProjectId;
    protected static String copiedProjectId;
    protected static String createdTemplateId;
    protected static String copiedTemplateId;

    @BeforeEach
    void setUp() throws Exception {
        // 确保测试输出目录存在
        String outputRootPath = configService.getOutputRootPath();
        Path outputPath = Paths.get(outputRootPath);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        // 每个测试后清理（可选，根据需要启用）
        // cleanTestOutputDir();
    }

    /**
     * 清理测试输出目录
     */
    protected void cleanTestOutputDir() throws IOException {
        String outputRootPath = configService.getOutputRootPath();
        Path outputPath = Paths.get(outputRootPath);
        
        if (outputPath != null && Files.exists(outputPath)) {
            Files.walk(outputPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    /**
     * 等待1毫秒，确保ID不冲突
     */
    protected void waitForIdGeneration() throws InterruptedException {
        Thread.sleep(1);
    }
}
