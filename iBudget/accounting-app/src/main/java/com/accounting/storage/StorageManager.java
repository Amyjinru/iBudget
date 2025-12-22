package com.accounting.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 存储管理器
 * 负责本地文件的读写操作
 */
public class StorageManager {
    private static final String DATA_DIR = "data";
    private Path dataPath;
    
    public StorageManager() {
        this.dataPath = Paths.get(DATA_DIR);
        try {
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
            }
        } catch (IOException e) {
            System.err.println("创建数据目录失败: " + e.getMessage());
        }
    }
    
    /**
     * 读取文件内容
     */
    public String readFile(String fileName) throws IOException {
        Path filePath = dataPath.resolve(fileName);
        if (!Files.exists(filePath)) {
            return null;
        }
        return Files.readString(filePath);
    }
    
    /**
     * 写入文件内容
     */
    public void writeFile(String fileName, String content) throws IOException {
        Path filePath = dataPath.resolve(fileName);
        Files.writeString(filePath, content);
    }
    
    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String fileName) {
        Path filePath = dataPath.resolve(fileName);
        return Files.exists(filePath);
    }
    
    /**
     * 删除文件
     */
    public boolean deleteFile(String fileName) throws IOException {
        Path filePath = dataPath.resolve(fileName);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            return true;
        }
        return false;
    }
    
    /**
     * 获取数据目录路径
     */
    public Path getDataPath() {
        return dataPath;
    }
}

