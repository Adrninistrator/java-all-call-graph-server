package com.github.adrninistrator.jacgserver.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ID生成工具类
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    private static final SimpleDateFormat ID_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    /**
     * 生成ID（使用当前时间精确到毫秒的字符串形式：yyyyMMddHHmmssSSS）
     *
     * @return ID字符串
     */
    public static synchronized String generateId() {
        return ID_DATE_FORMAT.format(new Date());
    }

    /**
     * 获取当前时间字符串
     *
     * @return 时间字符串
     */
    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    /**
     * 格式化时间戳为字符串
     *
     * @param timestamp 时间戳（毫秒）
     * @return 时间字符串
     */
    public static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }

    /**
     * 格式化执行耗时为可读字符串
     *
     * @param durationMs 执行耗时（毫秒）
     * @return 格式化的耗时字符串，如 "1分30秒" 或 "30秒"
     */
    public static String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        if (seconds < 60) {
            return seconds + "秒";
        }
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (remainingSeconds == 0) {
            return minutes + "分钟";
        }
        return minutes + "分" + remainingSeconds + "秒";
    }
}
