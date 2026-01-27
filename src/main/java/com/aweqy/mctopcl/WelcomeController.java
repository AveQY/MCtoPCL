package com.aweqy.mctopcl;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class WelcomeController {
    
    @FXML
    protected void onSelectDirectory(ActionEvent event) {
        // 创建目录选择器
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("请选择PCL文件目录");
        
        // 显示目录选择对话框
        File selectedDirectory = directoryChooser.showDialog(null);
        
        if (selectedDirectory != null) {
            // 目录选择成功，切换到版本选择界面
            try {
                // 加载版本选择FXML
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("versions-view.fxml"));
                Parent root = fxmlLoader.load();
                
                // 获取版本选择控制器
                VersionsController controller = fxmlLoader.getController();
                // 传递选中的目录
                controller.setPclDirectory(selectedDirectory);
                
                // 创建版本选择场景
                Scene scene = new Scene(root, 700, 600);
                
                // 获取当前舞台并设置新场景
                Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                stage.setTitle("MC存档管理工具");
                stage.setScene(scene);
                stage.setResizable(false);
                stage.show();
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
