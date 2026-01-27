package com.aweqy.mctopcl;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VersionsController {
    
    @FXML
    private ListView<String> versionsList;
    
    private File pclDirectory;
    
    /**
     * 设置PCL目录
     * @param directory PCL目录
     */
    public void setPclDirectory(File directory) {
        this.pclDirectory = directory;
        loadVersions();
    }
    
    /**
     * 加载版本列表
     */
    private void loadVersions() {
        // 检查.minecraft/versions文件夹是否存在
        File minecraftDir = new File(pclDirectory, ".minecraft");
        File versionsDir = new File(minecraftDir, "versions");
        
        if (!minecraftDir.exists() || !versionsDir.exists()) {
            // 不存在，返回欢迎页面
            showErrorAndBackToWelcome("PCL启动检测错误！", "目录结构不正确", ".minecraft/versions文件夹不存在，请重新选择PCL文件目录。");
            return;
        }
        
        // 读取versions文件夹下的版本目录
        List<String> versions = new ArrayList<>();
        File[] versionDirs = versionsDir.listFiles(File::isDirectory);
        
        if (versionDirs != null) {
            for (File versionDir : versionDirs) {
                versions.add(versionDir.getName());
            }
        }
        
        if (versions.isEmpty()) {
            // 没有版本目录，返回欢迎页面
            showErrorAndBackToWelcome("PCL启动检测错误！", "没有找到版本", "versions文件夹中没有找到游戏版本，请重新选择PCL文件目录。");
            return;
        }
        
        // 显示版本列表
        versionsList.getItems().clear();
        versionsList.getItems().addAll(versions);
    }
    
    /**
     * 显示错误信息并返回欢迎页面
     */
    private void showErrorAndBackToWelcome(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        // 移除内容文本
        alert.setContentText("");
        
        // 美化对话框
        javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: #f5f5f5;"
        );
        
        javafx.scene.control.Label headerLabel = (javafx.scene.control.Label) dialogPane.lookup(".header-panel .label");
        if (headerLabel != null) {
            headerLabel.setStyle(
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #333;"
            );
        }
        
        javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialogPane.lookup(".button-bar .button");
        if (okButton != null) {
            okButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 6 12; " +
                "-fx-border-radius: 4;"
            );
        }
        
        alert.showAndWait();
        
        // 返回欢迎页面
        onBackToWelcome(null);
    }
    
    @FXML
    protected void onSelectVersion(ActionEvent event) {
        String selectedVersion = versionsList.getSelectionModel().getSelectedItem();
        if (selectedVersion == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText("请选择版本");
            alert.setContentText("请从列表中选择一个游戏版本。");
            
            // 美化对话框
            javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle(
                "-fx-background-color: #f5f5f5;"
            );
            
            javafx.scene.control.Label headerLabel = (javafx.scene.control.Label) dialogPane.lookup(".header-panel .label");
            if (headerLabel != null) {
                headerLabel.setStyle(
                    "-fx-font-size: 16px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-text-fill: #333;"
                );
            }
            
            javafx.scene.control.Label contentLabel = (javafx.scene.control.Label) dialogPane.lookup(".content.label");
            if (contentLabel != null) {
                contentLabel.setStyle(
                    "-fx-font-size: 14px; " +
                    "-fx-text-fill: #666;"
                );
            }
            
            javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialogPane.lookup(".button-bar .button");
            if (okButton != null) {
                okButton.setStyle(
                    "-fx-background-color: #4CAF50; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 6 12; " +
                    "-fx-border-radius: 4;"
                );
            }
            
            alert.showAndWait();
            return;
        }
        
        // 读取该版本目录下的saves目录
        File minecraftDir = new File(pclDirectory, ".minecraft");
        File versionDir = new File(minecraftDir, "versions" + File.separator + selectedVersion);
        File savesDir = new File(versionDir, "saves");
        
        if (!savesDir.exists()) {
            // saves目录不存在，返回欢迎页面
            showErrorAndBackToWelcome("PCL启动检测错误！", "saves目录不存在", "该版本目录下没有saves文件夹，请重新选择PCL文件目录。");
            return;
        }
        
        // 保存配置信息
        ConfigManager.savePclDirectory(pclDirectory.getAbsolutePath());
        ConfigManager.saveSelectedVersion(selectedVersion);
        ConfigManager.saveSavesDirectory(savesDir.getAbsolutePath());
        
        // 跳转到主应用界面，传递saves目录
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
            Parent root = fxmlLoader.load();
            
            HelloController controller = fxmlLoader.getController();
            controller.setSelectedDirectory(savesDir);
            
            Scene scene = new Scene(root, 700, 600);
            
            Stage stage = (Stage) versionsList.getScene().getWindow();
            stage.setTitle("MC存档管理工具");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    protected void onBackToWelcome(ActionEvent event) {
        // 跳转到欢迎页面
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("welcome-view.fxml"));
            Parent root = fxmlLoader.load();
            
            Scene scene = new Scene(root, 700, 600);
            
            Stage stage = (Stage) versionsList.getScene().getWindow();
            stage.setTitle("MC存档管理工具");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
