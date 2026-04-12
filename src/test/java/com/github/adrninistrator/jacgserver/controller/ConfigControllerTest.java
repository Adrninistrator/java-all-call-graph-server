package com.github.adrninistrator.jacgserver.controller;

import com.github.adrninistrator.jacgserver.BaseTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 配置参数控制器测试
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ConfigControllerTest extends BaseTest {

    /**
     * 测试获取配置参数定义
     */
    @Test
    void testGetConfigDefinitions() throws Exception {
        mockMvc.perform(get("/api/v1/config/definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.javaCG2").exists())
                .andExpect(jsonPath("$.data.jacg").exists())
                .andExpect(jsonPath("$.data.javaCG2.mainConfig").isArray())
                .andExpect(jsonPath("$.data.javaCG2.listConfig").isArray())
                .andExpect(jsonPath("$.data.javaCG2.setConfig").isArray())
                .andExpect(jsonPath("$.data.jacg.mainConfig").isArray())
                .andExpect(jsonPath("$.data.jacg.dbConfig").isArray())
                .andExpect(jsonPath("$.data.jacg.listConfig").isArray())
                .andExpect(jsonPath("$.data.jacg.setConfig").isArray());
    }
}
