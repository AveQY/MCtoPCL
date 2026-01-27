package com.aweqy.mctopcl;

import java.io.*;
import java.util.Properties;

public class OnlineConfig {

    private static final String CONFIG_FILE = "online-config.properties";
    private static final Properties properties = new Properties();

    /**
     * 加载配置
     */
    public static void load() {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        } catch (IOException e) {
            // 配置文件不存在，使用默认值
            setDefaultValues();
        }
    }

    /**
     * 保存配置
     */
    public static void save() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "MCtoPCL Online Config");
        } catch (IOException e) {
            System.err.println("保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 设置默认值
     */
    private static void setDefaultValues() {
        properties.setProperty("server.address", "");
        properties.setProperty("server.port", "");
        properties.setProperty("connection.password", "");
        properties.setProperty("protocol.type", "TCP");
        properties.setProperty("local.address", "127.0.0.1");
        properties.setProperty("local.port", "");
        properties.setProperty("remote.port", "");
    }

    /**
     * 获取字符串配置
     */
    public static String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 设置字符串配置
     */
    public static void setString(String key, String value) {
        properties.setProperty(key, value);
    }
}
