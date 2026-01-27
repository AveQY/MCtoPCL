package com.aweqy.mctopcl;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // 检查是否存在有效的配置
        if (ConfigManager.isSavesDirectoryValid()) {
            // 直接加载主应用界面
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 700, 600);
            
            // 获取主应用控制器并设置saves目录
            HelloController controller = fxmlLoader.getController();
            String savesDirPath = ConfigManager.getSavesDirectory();
            if (savesDirPath != null) {
                controller.setSelectedDirectory(new java.io.File(savesDirPath));
            }
            
            // 设置应用图标
            stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream("app-icon.png")));
            
            stage.setTitle("MC存档管理工具");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } else {
            // 显示欢迎页面
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("welcome-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 700, 600);
            
            // 设置应用图标
            stage.getIcons().add(new Image(HelloApplication.class.getResourceAsStream("app-icon.png")));
            
            stage.setTitle("MC存档管理工具");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}