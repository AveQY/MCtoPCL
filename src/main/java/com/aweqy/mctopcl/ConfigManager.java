package com.aweqy.mctopcl;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";
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
            properties.store(output, "MC存档管理工具配置");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 保存PCL目录路径
     * @param pclDirPath PCL目录路径
     */
    public static void savePclDirectory(String pclDirPath) {
        properties.setProperty("pcl_directory", pclDirPath);
        saveConfig();
    }
    
    /**
     * 保存选中的版本
     * @param version 选中的版本
     */
    public static void saveSelectedVersion(String version) {
        properties.setProperty("selected_version", version);
        saveConfig();
    }
    
    /**
     * 保存saves目录路径
     * @param savesDirPath saves目录路径
     */
    public static void saveSavesDirectory(String savesDirPath) {
        properties.setProperty("saves_directory", savesDirPath);
        saveConfig();
    }
    
    /**
     * 获取保存的PCL目录路径
     * @return PCL目录路径
     */
    public static String getPclDirectory() {
        return properties.getProperty("pcl_directory");
    }
    
    /**
     * 获取保存的选中版本
     * @return 选中的版本
     */
    public static String getSelectedVersion() {
        return properties.getProperty("selected_version");
    }
    
    /**
     * 获取保存的saves目录路径
     * @return saves目录路径
     */
    public static String getSavesDirectory() {
        return properties.getProperty("saves_directory");
    }
    
    /**
     * 检查保存的saves目录是否存在
     * @return 是否存在
     */
    public static boolean isSavesDirectoryValid() {
        String savesDirPath = getSavesDirectory();
        if (savesDirPath == null) {
            return false;
        }
        File savesDir = new File(savesDirPath);
        return savesDir.exists() && savesDir.isDirectory();
    }
}
