package com.aweqy.mctopcl;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Arrays;
import java.util.zip.*;

public class HelloController {
    @FXML
    private Label statusLabel;
    @FXML
    private TextField directoryPath;
    @FXML
    private ListView<String> fileList;
    @FXML
    private ListView<String> cloudFileList;
    @FXML
    private javafx.scene.control.ProgressBar progressBar;
    @FXML
    private javafx.scene.layout.VBox progressBox;

    private File selectedDirectory;
    private Stage stage;

    @FXML
    public void initialize() {
        // 添加双击事件监听器
        fileList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedItem = fileList.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    handleFileDoubleClick(selectedItem);
                }
            }
        });
        
        // 初始化云存档列表
        initializeCloudFileList();
    }
    
    /**
     * 初始化云存档列表
     */
    private void initializeCloudFileList() {
        final List<String> cloudFiles = new ArrayList<>();
        
        // 检查是否有云存储配置
        if (CloudStorageConfig.hasConfig()) {
            // 获取所有配置名称
            List<String> configNames = CloudStorageConfig.getConfigNames();
            
            if (!configNames.isEmpty()) {
                // 使用第一个配置来获取文件列表
                String firstConfigName = configNames.get(0);
                Map<String, String> config = CloudStorageConfig.getConfig(firstConfigName);
                
                // 在后台线程中从WebDav服务器获取文件列表
                statusLabel.setText("正在加载WebDav文件...");
                
                new Thread(() -> {
                    try {
                        // 获取配置信息
                        String endpoint = config.get("endpoint");
                        String username = config.get("username");
                        String password = config.get("password");
                        String path = config.get("path");
                        
                        // 构建完整的WebDav URL
                        String webdavUrl = endpoint;
                        if (!webdavUrl.endsWith("/")) {
                            webdavUrl += "/";
                        }
                        if (path != null && !path.isEmpty() && !path.equals("/")) {
                            if (!path.startsWith("/")) {
                                webdavUrl += path;
                            } else {
                                webdavUrl += path.substring(1);
                            }
                            if (!webdavUrl.endsWith("/")) {
                                webdavUrl += "/";
                            }
                        }
                        
                        // 创建Sardine客户端
                        com.github.sardine.Sardine sardine = com.github.sardine.SardineFactory.begin(username, password);
                        
                        // 获取文件列表
                        List<com.github.sardine.DavResource> resources = sardine.list(webdavUrl);
                        
                        // 提取文件名称
                        final List<String> webdavFiles = new ArrayList<>();
                        for (com.github.sardine.DavResource resource : resources) {
                            // 只添加文件，跳过目录
                            if (!resource.isDirectory()) {
                                String fileName = resource.getName();
                                webdavFiles.add(fileName);
                            }
                        }
                        
                        // 关闭Sardine客户端
                        sardine.shutdown();
                        
                        // 更新UI
                        Platform.runLater(() -> {
                            cloudFiles.addAll(webdavFiles);
                            cloudFileList.getItems().clear();
                            cloudFileList.getItems().addAll(cloudFiles);
                            statusLabel.setText("WebDav文件加载成功！");
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            statusLabel.setText("加载WebDav文件失败：" + e.getMessage());
                        });
                    }
                }).start();
                
                // 添加云存档的双击事件
                cloudFileList.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        String selectedItem = cloudFileList.getSelectionModel().getSelectedItem();
                        if (selectedItem != null && !selectedItem.equals("请添加云存储配置")) {
                            handleCloudFileDoubleClick(selectedItem);
                        }
                    }
                });
                
                return; // 提前返回，因为后台线程会更新UI
            }
        } else {
            // 没有配置，提示用户添加
            cloudFiles.add("请添加云存储配置");
        }
        
        cloudFileList.getItems().clear();
        cloudFileList.getItems().addAll(cloudFiles);
        
        // 添加云存档的双击事件
        cloudFileList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedItem = cloudFileList.getSelectionModel().getSelectedItem();
                if (selectedItem != null && !selectedItem.equals("请添加云存储配置")) {
                    handleCloudFileDoubleClick(selectedItem);
                }
            }
        });
    }
    
    /**
     * 处理云存档文件的双击事件
     * @param fileName 云存档文件名
     */
    private void handleCloudFileDoubleClick(String fileName) {
        // 显示操作选择对话框
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("云存档操作");
        alert.setHeaderText("请选择操作类型");
        alert.setContentText("您选择的文件: " + fileName + "\n本地路径: " + (selectedDirectory != null ? selectedDirectory.getAbsolutePath() : "未选择目录"));
        
        // 添加自定义按钮
        javafx.scene.control.ButtonType downloadButton = new javafx.scene.control.ButtonType("下载存档");
        javafx.scene.control.ButtonType deleteButton = new javafx.scene.control.ButtonType("删除文件");
        javafx.scene.control.ButtonType cancelButton = new javafx.scene.control.ButtonType("取消", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(downloadButton, deleteButton, cancelButton);
        
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
        
        // 美化按钮
        dialogPane.getButtonTypes().stream()
            .map(dialogPane::lookupButton)
            .filter(java.util.Objects::nonNull)
            .forEach(node -> {
                if (node instanceof javafx.scene.control.Button) {
                    javafx.scene.control.Button button = (javafx.scene.control.Button) node;
                    if (button.getText().equals("下载存档")) {
                        button.setStyle(
                            "-fx-background-color: #4CAF50; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 6 12; " +
                            "-fx-border-radius: 4;"
                        );
                    } else if (button.getText().equals("删除文件")) {
                        button.setStyle(
                            "-fx-background-color: #f44336; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 6 12; " +
                            "-fx-border-radius: 4;"
                        );
                    } else if (button.getText().equals("取消")) {
                        button.setStyle(
                            "-fx-background-color: #f5f5f5; " +
                            "-fx-text-fill: #666; " +
                            "-fx-border-color: #ddd; " +
                            "-fx-padding: 6 12; " +
                            "-fx-border-radius: 4;"
                        );
                    }
                }
            });
        
        // 获取对话框结果
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            // 检查是否有云存储配置
            if (!CloudStorageConfig.hasConfig()) {
                statusLabel.setText("错误: 请先配置云存储");
                return;
            }
            
            if (result.get() == downloadButton) {
                // 下载存档
                // 检查本地目录是否已选择
                if (selectedDirectory == null) {
                    statusLabel.setText("错误: 请先选择本地目录");
                    return;
                }
                
                // 开始下载
                statusLabel.setText("正在下载云存档: " + fileName);
                
                // 在后台线程中执行下载
                new Thread(() -> {
                    try {
                        // 获取云存储配置
                        List<String> configNames = CloudStorageConfig.getConfigNames();
                        String firstConfigName = configNames.get(0);
                        Map<String, String> config = CloudStorageConfig.getConfig(firstConfigName);
                        
                        // 获取配置信息
                        String endpoint = config.get("endpoint");
                        String username = config.get("username");
                        String password = config.get("password");
                        String path = config.get("path");
                        
                        // 构建完整的WebDav URL
                        String webdavUrl = endpoint;
                        if (!webdavUrl.endsWith("/")) {
                            webdavUrl += "/";
                        }
                        if (path != null && !path.isEmpty() && !path.equals("/")) {
                            if (!path.startsWith("/")) {
                                webdavUrl += path;
                            } else {
                                webdavUrl += path.substring(1);
                            }
                            if (!webdavUrl.endsWith("/")) {
                                webdavUrl += "/";
                            }
                        }
                        webdavUrl += fileName;
                        
                        // 创建Sardine客户端
                        com.github.sardine.Sardine sardine = com.github.sardine.SardineFactory.begin(username, password);
                        
                        // 下载文件到本地目录
                        File localFile = new File(selectedDirectory, fileName);
                        try (InputStream is = sardine.get(webdavUrl);
                             FileOutputStream fos = new FileOutputStream(localFile)) {
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = is.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                        
                        // 关闭Sardine客户端
                        sardine.shutdown();
                        
                        // 刷新本地文件列表
                        Platform.runLater(() -> {
                            loadFiles(selectedDirectory);
                            statusLabel.setText("存档下载成功: " + fileName);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            statusLabel.setText("下载云存档失败: " + e.getMessage());
                        });
                    }
                }).start();
            } else if (result.get() == deleteButton) {
                // 删除文件
                // 显示确认对话框
                Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
                confirmAlert.setTitle("删除文件");
                confirmAlert.setHeaderText("确定要删除这个云存档吗？");
                confirmAlert.setContentText("文件: " + fileName + "\n删除后无法恢复！");
                
                // 美化确认对话框
                javafx.scene.control.DialogPane confirmDialogPane = confirmAlert.getDialogPane();
                confirmDialogPane.setStyle("-fx-background-color: #f5f5f5;");
                
                // 美化按钮
                confirmDialogPane.getButtonTypes().stream()
                    .map(confirmDialogPane::lookupButton)
                    .filter(java.util.Objects::nonNull)
                    .forEach(node -> {
                        if (node instanceof javafx.scene.control.Button) {
                            javafx.scene.control.Button button = (javafx.scene.control.Button) node;
                            if (button.getText().equals("确定")) {
                                button.setStyle(
                                    "-fx-background-color: #f44336; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-padding: 6 12; " +
                                    "-fx-border-radius: 4;"
                                );
                            } else if (button.getText().equals("取消")) {
                                button.setStyle(
                                    "-fx-background-color: #f5f5f5; " +
                                    "-fx-text-fill: #666; " +
                                    "-fx-border-color: #ddd; " +
                                    "-fx-padding: 6 12; " +
                                    "-fx-border-radius: 4;"
                                );
                            }
                        }
                    });
                
                // 获取确认对话框结果
                java.util.Optional<javafx.scene.control.ButtonType> confirmResult = confirmAlert.showAndWait();
                if (confirmResult.isPresent() && confirmResult.get() == javafx.scene.control.ButtonType.OK) {
                    // 执行删除操作
                    statusLabel.setText("正在删除云存档: " + fileName);
                    
                    // 在后台线程中执行删除
                    new Thread(() -> {
                        try {
                            // 获取云存储配置
                            List<String> configNames = CloudStorageConfig.getConfigNames();
                            String firstConfigName = configNames.get(0);
                            Map<String, String> config = CloudStorageConfig.getConfig(firstConfigName);
                            
                            // 获取配置信息
                            String endpoint = config.get("endpoint");
                            String username = config.get("username");
                            String password = config.get("password");
                            String path = config.get("path");
                            
                            // 构建完整的WebDav URL
                            String webdavUrl = endpoint;
                            if (!webdavUrl.endsWith("/")) {
                                webdavUrl += "/";
                            }
                            if (path != null && !path.isEmpty() && !path.equals("/")) {
                                if (!path.startsWith("/")) {
                                    webdavUrl += path;
                                } else {
                                    webdavUrl += path.substring(1);
                                }
                                if (!webdavUrl.endsWith("/")) {
                                    webdavUrl += "/";
                                }
                            }
                            webdavUrl += fileName;
                            
                            System.out.println("删除URL: " + webdavUrl);
                            
                            // 创建Sardine客户端
                            com.github.sardine.Sardine sardine = com.github.sardine.SardineFactory.begin(username, password);
                            
                            // 删除文件
                            sardine.delete(webdavUrl);
                            System.out.println("文件删除成功！");
                            
                            // 关闭Sardine客户端
                            sardine.shutdown();
                            
                            // 刷新云存档列表
                            Platform.runLater(() -> {
                                initializeCloudFileList();
                                statusLabel.setText("云存档删除成功: " + fileName);
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            final String errorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
                            Platform.runLater(() -> {
                                statusLabel.setText("删除失败: " + errorMessage);
                                // 显示详细错误信息对话框
                                Alert errorAlert = new Alert(AlertType.ERROR);
                                errorAlert.setTitle("删除失败");
                                errorAlert.setHeaderText("无法删除云存档");
                                errorAlert.setContentText("错误原因: " + errorMessage + "\n\n请检查:\n1. 网络连接是否正常\n2. WebDav服务器地址是否正确\n3. 用户名和密码是否正确\n4. 服务器是否可以访问\n5. 服务器是否有删除权限");
                                errorAlert.showAndWait();
                            });
                        }
                    }).start();
                }
            }
        }
    }
    
    /**
     * 处理刷新云存档文件列表的按钮点击事件
     */
    @FXML
    protected void onRefreshCloudFiles() {
        statusLabel.setText("正在刷新云存档文件列表...");
        initializeCloudFileList();
    }
    
    /**
     * 处理联机按钮点击事件
     */
    @FXML
    protected void onConnectOnline() {
        // 打开联机界面
        try {
            System.out.println("尝试打开联机界面...");
            
            // 检查 FXML 文件是否存在
            java.net.URL fxmlUrl = getClass().getResource("online-view.fxml");
            if (fxmlUrl == null) {
                throw new Exception("找不到 online-view.fxml 文件");
            }
            System.out.println("找到 FXML 文件：" + fxmlUrl);
            
            javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(fxmlUrl);
            javafx.scene.Parent root = fxmlLoader.load();
            
            OnlineController controller = fxmlLoader.getController();
            System.out.println("获取到 OnlineController 实例：" + controller);
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 650, 600);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("内网穿透设置");
            stage.setScene(scene);
            stage.setResizable(false);
            
            controller.setStage(stage);
            System.out.println("设置舞台完成，准备显示界面");
            stage.showAndWait();
            
            System.out.println("联机界面已关闭");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("打开联机界面失败：" + e.getMessage());
            statusLabel.setText("打开联机界面失败：" + e.getMessage());
        }
    }
    
    /**
     * 处理配置云存储的按钮点击事件
     */
    @FXML
    protected void onConfigureCloudStorage() {
        // 弹出配置云存储的对话框
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("cloud-storage-dialog.fxml"));
            javafx.scene.Parent root = fxmlLoader.load();
            
            CloudStorageDialogController controller = fxmlLoader.getController();
            
            // 检查是否有现有配置，如果有则加载第一个配置
            List<String> configNames = CloudStorageConfig.getConfigNames();
            if (!configNames.isEmpty()) {
                String firstConfigName = configNames.get(0);
                Map<String, String> config = CloudStorageConfig.getConfig(firstConfigName);
                // 加载现有配置到对话框
                controller.setConfig(
                    firstConfigName,
                    config.get("endpoint"),
                    config.get("username"),
                    config.get("password"),
                    config.get("path")
                );
            }
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 450, 300);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("配置云存储");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();
            
            // 配置添加或更新成功后，重新初始化云存档列表
            if (controller.isConfirmed()) {
                initializeCloudFileList();
                statusLabel.setText("云存储配置更新成功！");
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("打开配置对话框失败：" + e.getMessage());
        }
    }

    @FXML
    protected void onSelectVersion() {
        // 跳转到版本选择界面
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("versions-view.fxml"));
            javafx.scene.Parent root = fxmlLoader.load();
            
            VersionsController controller = fxmlLoader.getController();
            
            // 获取当前saves目录的上级目录结构，重建PCL目录路径
            // 假设当前路径是：[PCL目录]/.minecraft/versions/[版本]/saves
            File currentDir = selectedDirectory;
            if (currentDir != null) {
                // 向上两级到versions目录
                File versionDir = currentDir.getParentFile();
                if (versionDir != null) {
                    File versionsDir = versionDir.getParentFile();
                    if (versionsDir != null) {
                        File minecraftDir = versionsDir.getParentFile();
                        if (minecraftDir != null) {
                            File pclDir = minecraftDir.getParentFile();
                            if (pclDir != null) {
                                controller.setPclDirectory(pclDir);
                            }
                        }
                    }
                }
            }
            
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 700, 600);
            
            if (stage == null) {
                javafx.scene.Scene currentScene = statusLabel.getScene();
                if (currentScene != null) {
                    stage = (javafx.stage.Stage) currentScene.getWindow();
                } else {
                    stage = new javafx.stage.Stage();
                }
            }
            
            stage.setTitle("MC存档管理工具");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
            
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置选中的目录
     * @param directory 选中的目录
     */
    public void setSelectedDirectory(File directory) {
        this.selectedDirectory = directory;
        if (selectedDirectory != null) {
            directoryPath.setText(selectedDirectory.getAbsolutePath());
            statusLabel.setText("已选择目录: " + selectedDirectory.getName());
            loadFiles(selectedDirectory);
        }
    }

    private void loadFiles(File directory) {
        if (directory == null) {
            return;
        }
        
        List<String> files = new ArrayList<>();
        try {
            Files.list(directory.toPath())
                 .forEach(path -> {
                     File file = path.toFile();
                     files.add(file.getName());
                 });
            
            fileList.getItems().clear();
            fileList.getItems().addAll(files);
            statusLabel.setText("已加载 " + files.size() + " 个文件/文件夹");
        } catch (IOException e) {
            statusLabel.setText("加载文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadFiles() {
        loadFiles(selectedDirectory);
    }

    private void handleFileDoubleClick(String fileName) {
        // 创建选择对话框
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("选择操作");
        alert.setHeaderText("请选择操作类型");
        alert.setContentText("您选择的文件: " + fileName);
        
        // 添加自定义按钮
        javafx.scene.control.ButtonType backupButton = new javafx.scene.control.ButtonType("备份存档");
        javafx.scene.control.ButtonType uploadButton = new javafx.scene.control.ButtonType("上传存档");
        javafx.scene.control.ButtonType deleteButton = new javafx.scene.control.ButtonType("删除文件");
        javafx.scene.control.ButtonType cancelButton = new javafx.scene.control.ButtonType("取消", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(backupButton, uploadButton, deleteButton, cancelButton);
        
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
        
        // 美化按钮
        dialogPane.getButtonTypes().stream()
            .map(dialogPane::lookupButton)
            .filter(java.util.Objects::nonNull)
            .forEach(node -> {
                if (node instanceof javafx.scene.control.Button) {
                    javafx.scene.control.Button button = (javafx.scene.control.Button) node;
                    if (button.getText().equals("备份存档")) {
                        button.setStyle(
                            "-fx-background-color: #4CAF50; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 6 12; " +
                            "-fx-border-radius: 4;"
                        );
                    } else if (button.getText().equals("上传存档")) {
                        button.setStyle(
                            "-fx-background-color: #2196F3; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 6 12; " +
                            "-fx-border-radius: 4;"
                        );
                    } else if (button.getText().equals("删除文件")) {
                        button.setStyle(
                            "-fx-background-color: #f44336; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 6 12; " +
                            "-fx-border-radius: 4;"
                        );
                    } else if (button.getText().equals("取消")) {
                        button.setStyle(
                            "-fx-background-color: #f5f5f5; " +
                            "-fx-text-fill: #666; " +
                            "-fx-border-color: #ddd; " +
                            "-fx-padding: 6 12; " +
                            "-fx-border-radius: 4;"
                        );
                    }
                }
            });
        
        // 获取对话框结果
        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            // 判断双击的是文件还是目录
            Path itemPath = selectedDirectory.toPath().resolve(fileName);
            File itemFile = itemPath.toFile();
            
            if (result.get() == backupButton) {
                // 备份存档 - 与之前的逻辑相同
                // 显示进度条并重置
                Platform.runLater(() -> {
                    progressBox.setVisible(true);
                    progressBar.setProgress(0);
                });
                
                if (itemFile.isDirectory()) {
                    // 压缩双击选中的目录
                    statusLabel.setText("正在压缩目录: " + fileName);
                    
                    // 使用线程处理压缩操作，避免UI阻塞
                    new Thread(() -> {
                        try {
                            // 创建backup目录
                            String backupDirPath = System.getProperty("user.dir") + "\\backup";
                            File backupDir = new File(backupDirPath);
                            if (!backupDir.exists()) {
                                backupDir.mkdirs();
                            }
                            
                            // 生成压缩文件名
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd");
                            String dateStr = sdf.format(new Date());
                            String zipFileName = backupDirPath + "\\" + fileName + "_" + dateStr + ".zip";
                            
                            // 计算总文件数
                            final long[] totalFiles = {0};
                            Files.walk(itemFile.toPath())
                                 .filter(Files::isRegularFile)
                                 .forEach(p -> totalFiles[0]++);
                            
                            // 压缩选中的目录
                            final long[] processedFiles = {0};
                            compressDirectory(itemFile, zipFileName, processedFiles, totalFiles[0]);
                            
                            Platform.runLater(() -> {
                                statusLabel.setText("备份成功!\n压缩文件保存到: " + zipFileName);
                            });
                        } catch (Exception e) {
                            final String errorMessage = e.getMessage();
                            Platform.runLater(() -> {
                                statusLabel.setText("压缩失败: " + errorMessage);
                            });
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    // 压缩单个文件
                    statusLabel.setText("正在压缩文件: " + fileName);
                    
                    // 使用线程处理压缩操作，避免UI阻塞
                    new Thread(() -> {
                        try {
                            // 创建backup目录
                            String backupDirPath = System.getProperty("user.dir") + "\\backup";
                            File backupDir = new File(backupDirPath);
                            if (!backupDir.exists()) {
                                backupDir.mkdirs();
                            }
                            
                            // 生成压缩文件名
                            String folderName = selectedDirectory.getName();
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd");
                            String dateStr = sdf.format(new Date());
                            String zipFileName = backupDirPath + "\\" + folderName + "_" + dateStr + ".zip";
                            
                            // 压缩文件
                            try (FileOutputStream fos = new FileOutputStream(zipFileName);
                                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                                
                                // 创建压缩条目
                                String entryName = fileName;
                                ZipEntry zipEntry = new ZipEntry(entryName);
                                zos.putNextEntry(zipEntry);
                                
                                // 直接设置进度为1，因为单个文件只需要显示完成状态
                                Platform.runLater(() -> {
                                    progressBar.setProgress(1);
                                });
                                
                                // 复制文件内容
                                Files.copy(itemPath, zos);
                                
                                zos.closeEntry();
                            }
                            
                            Platform.runLater(() -> {
                                progressBar.setProgress(1);
                                statusLabel.setText("备份成功!\n压缩文件保存到: " + zipFileName);
                            });
                        } catch (Exception e) {
                            final String errorMessage = e.getMessage();
                            Platform.runLater(() -> {
                                statusLabel.setText("压缩失败: " + errorMessage);
                            });
                            e.printStackTrace();
                        }
                    }).start();
                }
            } else if (result.get() == uploadButton) {
                // 上传存档到WebDav
                // 检查是否有云存储配置
                if (!CloudStorageConfig.hasConfig()) {
                    statusLabel.setText("错误: 请先配置云存储");
                    return;
                }
                
                // 开始上传
                statusLabel.setText("正在上传存档: " + fileName);
                
                // 在后台线程中执行上传
                new Thread(() -> {
                    try {
                        // 获取云存储配置
                        List<String> configNames = CloudStorageConfig.getConfigNames();
                        String firstConfigName = configNames.get(0);
                        Map<String, String> config = CloudStorageConfig.getConfig(firstConfigName);
                        
                        // 获取配置信息
                        String endpoint = config.get("endpoint");
                        String username = config.get("username");
                        String password = config.get("password");
                        String path = config.get("path");
                        
                        // 构建完整的WebDav URL
                        String webdavUrl = endpoint;
                        if (!webdavUrl.endsWith("/")) {
                            webdavUrl += "/";
                        }
                        if (path != null && !path.isEmpty() && !path.equals("/")) {
                            if (!path.startsWith("/")) {
                                webdavUrl += path;
                            } else {
                                webdavUrl += path.substring(1);
                            }
                            if (!webdavUrl.endsWith("/")) {
                                webdavUrl += "/";
                            }
                        }
                        webdavUrl += fileName;
                        
                        System.out.println("上传URL: " + webdavUrl);
                        System.out.println("上传文件: " + itemFile.getAbsolutePath());
                        System.out.println("文件大小: " + itemFile.length() + " bytes");
                        
                        // 创建Sardine客户端
                        System.out.println("创建Sardine客户端...");
                        com.github.sardine.Sardine sardine = com.github.sardine.SardineFactory.begin(username, password);
                        
                        // 注意：Sardine 5.0版本不支持直接设置超时，使用默认超时
                        
                        // 上传文件
                        System.out.println("开始上传文件...");
                        
                        // 读取文件内容到内存，使用可重复的请求实体
                        byte[] fileContent = Files.readAllBytes(itemFile.toPath());
                        System.out.println("文件内容读取完成，大小: " + fileContent.length + " bytes");
                        
                        // 使用字节数组上传，这样是可重复的
                        sardine.put(webdavUrl, fileContent);
                        System.out.println("文件上传成功！");
                        
                        // 关闭Sardine客户端
                        sardine.shutdown();
                        
                        // 刷新云存档列表
                        Platform.runLater(() -> {
                            initializeCloudFileList();
                            statusLabel.setText("存档上传成功: " + fileName);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        final String errorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
                        Platform.runLater(() -> {
                            statusLabel.setText("上传存档失败: " + errorMessage);
                            // 显示详细错误信息对话框
                            Alert errorAlert = new Alert(AlertType.ERROR);
                            errorAlert.setTitle("上传失败");
                            errorAlert.setHeaderText("无法上传存档到WebDav服务器");
                            errorAlert.setContentText("错误原因: " + errorMessage + "\n\n请检查:\n1. WebDav服务器地址是否正确\n2. 用户名和密码是否正确\n3. 服务器是否可以访问\n4. 服务器是否有写入权限");
                            errorAlert.showAndWait();
                        });
                    }
                }).start();
            } else if (result.get() == deleteButton) {
                // 删除文件
                // 显示确认对话框
                Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
                confirmAlert.setTitle("删除文件");
                confirmAlert.setHeaderText("确定要删除这个文件吗？");
                confirmAlert.setContentText("文件: " + fileName + "\n删除后无法恢复！");
                
                // 美化确认对话框
                javafx.scene.control.DialogPane confirmDialogPane = confirmAlert.getDialogPane();
                confirmDialogPane.setStyle("-fx-background-color: #f5f5f5;");
                
                // 美化按钮
                confirmDialogPane.getButtonTypes().stream()
                    .map(confirmDialogPane::lookupButton)
                    .filter(java.util.Objects::nonNull)
                    .forEach(node -> {
                        if (node instanceof javafx.scene.control.Button) {
                            javafx.scene.control.Button button = (javafx.scene.control.Button) node;
                            if (button.getText().equals("确定")) {
                                button.setStyle(
                                    "-fx-background-color: #f44336; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-padding: 6 12; " +
                                    "-fx-border-radius: 4;"
                                );
                            } else if (button.getText().equals("取消")) {
                                button.setStyle(
                                    "-fx-background-color: #f5f5f5; " +
                                    "-fx-text-fill: #666; " +
                                    "-fx-border-color: #ddd; " +
                                    "-fx-padding: 6 12; " +
                                    "-fx-border-radius: 4;"
                                );
                            }
                        }
                    });
                
                // 获取确认对话框结果
                java.util.Optional<javafx.scene.control.ButtonType> confirmResult = confirmAlert.showAndWait();
                if (confirmResult.isPresent() && confirmResult.get() == javafx.scene.control.ButtonType.OK) {
                    // 执行删除操作
                    try {
                        if (itemFile.isDirectory()) {
                            // 删除目录（递归删除）
                            deleteDirectory(itemFile);
                        } else {
                            // 删除单个文件
                            Files.deleteIfExists(itemFile.toPath());
                        }
                        
                        // 刷新文件列表
                        loadFiles(selectedDirectory);
                        statusLabel.setText("文件删除成功: " + fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        statusLabel.setText("删除文件失败: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    private void compressDirectory(File directory, String zipFileName, long[] processedFiles, long totalFiles) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFileName);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            // 递归压缩目录
            compressDirectoryRecursive(directory, directory, zos, processedFiles, totalFiles);
        }
    }
    
    private void compressDirectoryRecursive(File rootDir, File currentDir, ZipOutputStream zos, long[] processedFiles, long totalFiles) throws IOException {
        File[] files = currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 递归压缩子目录
                    compressDirectoryRecursive(rootDir, file, zos, processedFiles, totalFiles);
                } else {
                    // 压缩文件
                    String relativePath = rootDir.toPath().relativize(file.toPath()).toString();
                    ZipEntry zipEntry = new ZipEntry(relativePath);
                    zos.putNextEntry(zipEntry);
                    
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                    
                    // 更新进度
                    processedFiles[0]++;
                    final double progress = (double) processedFiles[0] / totalFiles;
                    Platform.runLater(() -> {
                        progressBar.setProgress(progress);
                    });
                }
            }
        }
    }
    
    /**
     * 递归删除目录及其所有内容
     * @param directory 要删除的目录
     * @throws IOException 如果删除过程中发生错误
     */
    private void deleteDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    Files.deleteIfExists(file.toPath());
                }
            }
        }
        Files.deleteIfExists(directory.toPath());
    }
}