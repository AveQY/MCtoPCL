package com.aweqy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);
    private static final Properties properties = new Properties();
    private static final String CONFIG_FILE = "server.properties";
    
    /**
     * 加载配置文件
     */
    public static void load() {
        // 1. 首先尝试从当前目录加载配置文件
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
            logger.info("Config loaded successfully from current directory: {}", CONFIG_FILE);
            return;
        } catch (IOException e) {
            logger.warn("Config file not found in current directory: {}", e.getMessage());
        }
        
        // 2. 尝试从classpath加载配置文件
        try (InputStream is = ServerConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                properties.load(is);
                logger.info("Config loaded successfully from classpath: {}", CONFIG_FILE);
                return;
            }
        } catch (IOException e) {
            logger.warn("Config file not found in classpath: {}", e.getMessage());
        }
        
        // 3. 如果都加载失败，使用默认值
        logger.warn("No config file found, using default values");
        setDefaultValues();
    }
    
    /**
     * 设置默认配置值
     */
    private static void setDefaultValues() {
        properties.setProperty("server.port", "8080");
        properties.setProperty("server.password", "default_password");
        properties.setProperty("server.max_connections", "100");
        properties.setProperty("server.timeout", "30000");
        properties.setProperty("forward.buffer_size", "4096");
        properties.setProperty("log.level", "info");
    }
    
    /**
     * 获取字符串配置项
     */
    public static String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * 获取整数配置项
     */
    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }
    
    /**
     * 获取布尔配置项
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(properties.getProperty(key));
        } catch (NullPointerException e) {
            return defaultValue;
        }
    }
    
    /**
     * 获取长整型配置项
     */
    public static long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(properties.getProperty(key));
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }
}
