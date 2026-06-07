package com.github.adrninistrator.jacgserver.service;

import com.github.adrninistrator.jacgserver.BaseTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 查找模板目录功能测试
 * 验证统一后的findTemplateDir方法行为正确性
 * 包括：指定projectId精确查找、不指定projectId遍历查找、多项目同名模板场景
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FindTemplateDirTest extends BaseTest {

    private static final String PROJECT_A = "test_project_a";
    private static final String PROJECT_B = "test_project_b";
    private static final String TEMPLATE_SHARED = "shared_template";
    private static final String TEMPLATE_ONLY_A = "template_only_a";
    private static final String TEMPLATE_ONLY_B = "template_only_b";

    /**
     * 创建测试用的项目和模板目录结构
     * project_conf/
     *   test_project_a/
     *     templates/
     *       shared_template/
     *       template_only_a/
     *   test_project_b/
     *     templates/
     *       shared_template/
     *       template_only_b/
     */
    private void createTestData() throws IOException {
        String projectConfDir = configService.getProjectConfDir();

        // 项目A: shared_template + template_only_a
        Path projectA = Paths.get(projectConfDir, PROJECT_A);
        Path templatesA = projectA.resolve("templates");
        Files.createDirectories(templatesA.resolve(TEMPLATE_SHARED));
        Files.createDirectories(templatesA.resolve(TEMPLATE_ONLY_A));

        // 项目B: shared_template + template_only_b
        Path projectB = Paths.get(projectConfDir, PROJECT_B);
        Path templatesB = projectB.resolve("templates");
        Files.createDirectories(templatesB.resolve(TEMPLATE_SHARED));
        Files.createDirectories(templatesB.resolve(TEMPLATE_ONLY_B));
    }

    /**
     * 测试1: 指定projectId查找存在的模板，应返回正确路径
     */
    @Test
    @Order(1)
    void testFindWithProjectId_exists() throws IOException {
        createTestData();

        String result = configService.findTemplateDir(TEMPLATE_ONLY_A, PROJECT_A);
        assertNotNull(result, "指定projectId查找存在的模板应返回非null");
        assertTrue(result.contains(PROJECT_A), "返回路径应包含对应项目ID");
        assertTrue(result.contains(TEMPLATE_ONLY_A), "返回路径应包含模板ID");
    }

    /**
     * 测试2: 指定projectId查找不存在于该项目的模板，应返回null
     */
    @Test
    @Order(2)
    void testFindWithProjectId_notFoundInProject() throws IOException {
        String result = configService.findTemplateDir(TEMPLATE_ONLY_A, PROJECT_B);
        assertNull(result, "模板不存在于指定项目中应返回null");
    }

    /**
     * 测试3: 指定不存在的projectId，应返回null
     */
    @Test
    @Order(3)
    void testFindWithNonExistentProjectId() {
        String result = configService.findTemplateDir(TEMPLATE_ONLY_A, "non_existent_project");
        assertNull(result, "指定不存在的projectId应返回null");
    }

    /**
     * 测试4: 不指定projectId遍历查找唯一模板，应返回正确路径
     */
    @Test
    @Order(4)
    void testFindWithoutProjectId_uniqueTemplate() {
        String result = configService.findTemplateDir(TEMPLATE_ONLY_A, null);
        assertNotNull(result, "遍历查找唯一存在的模板应返回非null");
        assertTrue(result.contains(PROJECT_A), "返回路径应包含对应项目ID");
        assertTrue(result.contains(TEMPLATE_ONLY_A), "返回路径应包含模板ID");
    }

    /**
     * 测试5: 不指定projectId查找不存在的模板，应返回null
     */
    @Test
    @Order(5)
    void testFindWithoutProjectId_notFound() {
        String result = configService.findTemplateDir("non_existent_template", null);
        assertNull(result, "查找不存在的模板应返回null");
    }

    /**
     * 测试6: 不指定projectId查找同名模板时，应返回其中一个（不抛异常）
     * 验证在多项目同名模板场景下方法不会报错
     */
    @Test
    @Order(6)
    void testFindWithoutProjectId_duplicateTemplate() {
        // shared_template 存在于项目A和项目B中
        String result = configService.findTemplateDir(TEMPLATE_SHARED, null);
        assertNotNull(result, "多项目同名模板时应返回非null（找到其中一个）");
        assertTrue(result.contains(TEMPLATE_SHARED), "返回路径应包含模板ID");
        assertTrue(result.endsWith(TEMPLATE_SHARED) || result.contains(File.separator + TEMPLATE_SHARED),
                "返回路径应以模板ID结尾或在路径中包含模板ID");
    }

    /**
     * 测试7: 指定projectId查找同名模板时，应返回精确匹配的路径
     * 这验证了多项目同名模板时使用projectId可避免找错模板
     */
    @Test
    @Order(7)
    void testFindWithProjectId_duplicateTemplate() {
        String resultA = configService.findTemplateDir(TEMPLATE_SHARED, PROJECT_A);
        String resultB = configService.findTemplateDir(TEMPLATE_SHARED, PROJECT_B);

        assertNotNull(resultA, "项目A中查找同名模板应返回非null");
        assertNotNull(resultB, "项目B中查找同名模板应返回非null");
        assertTrue(resultA.contains(PROJECT_A), "项目A查找结果应包含项目A的路径");
        assertTrue(resultB.contains(PROJECT_B), "项目B查找结果应包含项目B的路径");
        assertNotEquals(resultA, resultB, "不同项目查找同名模板应返回不同路径");
    }

    /**
     * 测试8: templateId为null或空字符串，应返回null
     */
    @Test
    @Order(8)
    void testFindWithInvalidTemplateId() {
        assertNull(configService.findTemplateDir(null, PROJECT_A));
        assertNull(configService.findTemplateDir("", PROJECT_A));
        assertNull(configService.findTemplateDir("  ", PROJECT_A));
    }

    /**
     * 测试9: projectId为空字符串时，应退化为遍历查找
     */
    @Test
    @Order(9)
    void testFindWithEmptyProjectId() {
        String result = configService.findTemplateDir(TEMPLATE_ONLY_A, "");
        assertNotNull(result, "projectId为空字符串时应退化为遍历查找");
        assertTrue(result.contains(TEMPLATE_ONLY_A), "返回路径应包含模板ID");
    }

    /**
     * 清理测试数据
     */
    private void cleanupTestData() throws IOException {
        String projectConfDir = configService.getProjectConfDir();
        deleteDir(new File(projectConfDir, PROJECT_A));
        deleteDir(new File(projectConfDir, PROJECT_B));
    }

    private void deleteDir(File dir) {
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDir(f);
                }
                f.delete();
            }
        }
        dir.delete();
    }
}
