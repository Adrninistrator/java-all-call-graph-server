package com.github.adrninistrator.jacgserver.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * JSON工具类
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public final class JsonUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private JsonUtil() {
    }

    /**
     * 对象转JSON字符串
     *
     * @param obj 对象
     * @return JSON字符串
     */
    public static String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("对象转JSON失败", e);
            return null;
        }
    }

    /**
     * JSON字符串转对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类
     * @param <T>   泛型类型
     * @return 对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            logger.error("JSON转对象失败: json={}", json, e);
            return null;
        }
    }

    /**
     * 从文件读取JSON并转换为对象
     *
     * @param file  文件
     * @param clazz 目标类
     * @param <T>   泛型类型
     * @return 对象
     */
    public static <T> T fromFile(File file, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(file, clazz);
        } catch (IOException e) {
            logger.error("从文件读取JSON失败: file={}", file.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * 将对象写入JSON文件
     *
     * @param file 文件
     * @param obj  对象
     * @return 是否成功
     */
    public static boolean toFile(File file, Object obj) {
        try {
            OBJECT_MAPPER.writeValue(file, obj);
            return true;
        } catch (IOException e) {
            logger.error("写入JSON文件失败: file={}", file.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * 获取ObjectMapper实例
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
