package com.aweqy.mctopcl;

import java.io.*;
import java.util.*;

public class CloudStorageConfig {
    private static final String CONFIG_FILE = "cloud_storage_config.properties";
    private static final Properties properties = new Properties();
    
    static {
        // 加载配置文件
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException e) {
            // 配置文件不存在，忽略
        }
    }
    
    /**
     * 保存配置文件
     */
    private static void saveConfig() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "云存储配置");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 添加云存储配置（WebDav方式）
     * @param name 配置名称
     * @param endpoint WebDav服务器地址
     * @param username 用户名
     * @param password 密码
     * @param path WebDav路径
     */
    public static void addConfig(String name, String endpoint, String username, String password, String path) {
        String prefix = "cloud." + name + ".";
        properties.setProperty(prefix + "endpoint", endpoint);
        properties.setProperty(prefix + "username", username);
        properties.setProperty(prefix + "password", password);
        properties.setProperty(prefix + "path", path);
        properties.setProperty(prefix + "type", "webdav");
        saveConfig();
    }
    
    /**
     * 删除云存储配置
     * @param name 配置名称
     */
    public static void deleteConfig(String name) {
        String prefix = "cloud." + name + ".";
        
        // 移除所有相关属性
        List<String> keysToRemove = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                keysToRemove.add(key);
            }
        }
        
        for (String key : keysToRemove) {
            properties.remove(key);
        }
        
        saveConfig();
    }
    
    /**
     * 获取所有云存储配置名称
     * @return 配置名称列表
     */
    public static List<String> getConfigNames() {
        Set<String> names = new HashSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("cloud.")) {
                String name = key.split("\\.")[1];
                names.add(name);
            }
        }
        return new ArrayList<>(names);
    }
    
    /**
     * 获取云存储配置
     * @param name 配置名称
     * @return 配置信息，包含endpoint、username、password、path、type
     */
    public static Map<String, String> getConfig(String name) {
        Map<String, String> config = new HashMap<>();
        String prefix = "cloud." + name + ".";
        
        config.put("endpoint", properties.getProperty(prefix + "endpoint"));
        config.put("username", properties.getProperty(prefix + "username"));
        config.put("password", properties.getProperty(prefix + "password"));
        config.put("path", properties.getProperty(prefix + "path"));
        config.put("type", properties.getProperty(prefix + "type", "webdav"));
        
        return config;
    }
    
    /**
     * 检查是否存在云存储配置
     * @return 是否存在配置
     */
    public static boolean hasConfig() {
        return !getConfigNames().isEmpty();
    }
}
