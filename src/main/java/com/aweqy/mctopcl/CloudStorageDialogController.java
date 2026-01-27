package com.aweqy.mctopcl;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CloudStorageDialogController {
    @FXML
    private TextField configName;
    @FXML
    private TextField endpoint;
    @FXML
    private TextField username;
    @FXML
    private TextField password;
    @FXML
    private TextField path;
    
    private boolean confirmed = false;
    
    /**
     * 设置配置信息到对话框
     * @param configName 配置名称
     * @param endpoint WebDav服务器地址
     * @param username 用户名
     * @param password 密码
     * @param path WebDav路径
     */
    public void setConfig(String configName, String endpoint, String username, String password, String path) {
        this.configName.setText(configName);
        this.endpoint.setText(endpoint);
        this.username.setText(username);
        this.password.setText(password);
        this.path.setText(path);
    }
    
    /**
     * 处理取消按钮点击事件
     */
    @FXML
    protected void onCancel() {
        confirmed = false;
        closeDialog();
    }
    
    /**
     * 处理确定按钮点击事件
     */
    @FXML
    protected void onConfirm() {
        // 验证输入
        if (validateInput()) {
            confirmed = true;
            // 保存配置，路径为空时使用根目录
            String webdavPath = path.getText().isEmpty() ? "/" : path.getText();
            CloudStorageConfig.addConfig(
                configName.getText(),
                endpoint.getText(),
                username.getText(),
                password.getText(),
                webdavPath
            );
            closeDialog();
        }
    }
    
    /**
     * 验证输入
     * @return 是否验证通过
     */
    private boolean validateInput() {
        if (configName.getText().isEmpty()) {
            showError("配置名称不能为空");
            return false;
        }
        if (endpoint.getText().isEmpty()) {
            showError("WebDav服务器地址不能为空");
            return false;
        }
        if (username.getText().isEmpty()) {
            showError("用户名不能为空");
            return false;
        }
        if (password.getText().isEmpty()) {
            showError("密码不能为空");
            return false;
        }
        // WebDav路径可选，为空时使用根目录
        return true;
    }
    
    /**
     * 显示错误信息
     * @param message 错误信息
     */
    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // 美化对话框
        javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-radius: 4;");
        
        alert.showAndWait();
    }
    
    /**
     * 关闭对话框
     */
    private void closeDialog() {
        Stage stage = (Stage) configName.getScene().getWindow();
        stage.close();
    }
    
    /**
     * 检查是否确认
     * @return 是否确认
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}
