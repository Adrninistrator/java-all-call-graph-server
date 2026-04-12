package com.github.adrninistrator.jacgserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件工具类
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public final class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    private FileUtil() {
    }

    /**
     * 创建目录
     *
     * @param dirPath 目录路径
     * @return 是否成功
     */
    public static boolean createDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean result = dir.mkdirs();
            if (result) {
                logger.info("创建目录成功: {}", dirPath);
            } else {
                logger.error("创建目录失败: {}", dirPath);
            }
            return result;
        }
        return true;
    }

    /**
     * 删除目录及其内容
     *
     * @param dirPath 目录路径
     * @return 是否成功
     */
    public static boolean deleteDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            return true;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file.getAbsolutePath());
                } else {
                    if (!file.delete()) {
                        logger.error("删除文件失败: {}", file.getAbsolutePath());
                    }
                }
            }
        }
        boolean result = dir.delete();
        if (result) {
            logger.info("删除目录成功: {}", dirPath);
        } else {
            logger.error("删除目录失败: {}", dirPath);
        }
        return result;
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static String readFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\r\n");
            }
        } catch (IOException e) {
            logger.error("读取文件失败: {}", filePath, e);
            return null;
        }
        return content.toString();
    }

    /**
     * 写入文件内容
     *
     * @param filePath 文件路径
     * @param content  内容
     * @return 是否成功
     */
    public static boolean writeFile(String filePath, String content) {
        // 先写入临时文件
        String tempPath = filePath + ".tmp";
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(tempPath), StandardCharsets.UTF_8))) {
            writer.write(content);
        } catch (IOException e) {
            logger.error("写入临时文件失败: {}", tempPath, e);
            return false;
        }
        // 重命名为目标文件
        File tempFile = new File(tempPath);
        File targetFile = new File(filePath);
        if (targetFile.exists()) {
            if (!targetFile.delete()) {
                logger.error("删除目标文件失败: {}", filePath);
                return false;
            }
        }
        boolean result = tempFile.renameTo(targetFile);
        if (result) {
            logger.info("写入文件成功: {}", filePath);
        } else {
            logger.error("重命名文件失败: {} -> {}", tempPath, filePath);
        }
        return result;
    }

    /**
     * 读取文件最后N行
     *
     * @param filePath 文件路径
     * @param lines    行数
     * @return 文件内容列表
     */
    public static List<String> readLastLines(String filePath, int lines) {
        List<String> result = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            long fileLength = file.length();
            long position = fileLength - 1;
            int lineCount = 0;
            StringBuilder line = new StringBuilder();

            while (position >= 0 && lineCount < lines) {
                file.seek(position);
                char c = (char) file.read();
                if (c == '\n') {
                    if (line.length() > 0) {
                        result.add(0, line.reverse().toString());
                        line = new StringBuilder();
                        lineCount++;
                    }
                } else {
                    line.append(c);
                }
                position--;
            }
            // 处理最后一行
            if (line.length() > 0 && lineCount < lines) {
                result.add(0, line.reverse().toString());
            }
        } catch (IOException e) {
            logger.error("读取文件最后N行失败: {}", filePath, e);
        }
        return result;
    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    public static boolean exists(String filePath) {
        return new File(filePath).exists();
    }

    /**
     * 获取文件大小
     *
     * @param filePath 文件路径
     * @return 文件大小（字节）
     */
    public static long getFileSize(String filePath) {
        File file = new File(filePath);
        return file.exists() ? file.length() : 0;
    }

    /**
     * 复制目录
     *
     * @param sourceDir 源目录
     * @param targetDir 目标目录
     * @return 是否成功
     */
    public static boolean copyDirectory(String sourceDir, String targetDir) {
        try {
            Path source = Paths.get(sourceDir);
            Path target = Paths.get(targetDir);
            Files.walk(source).forEach(path -> {
                try {
                    Path dest = target.resolve(source.relativize(path));
                    Files.copy(path, dest);
                } catch (IOException e) {
                    logger.error("复制文件失败: {}", path, e);
                }
            });
            return true;
        } catch (IOException e) {
            logger.error("复制目录失败: {} -> {}", sourceDir, targetDir, e);
            return false;
        }
    }
}
