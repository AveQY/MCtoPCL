package com.aweqy.mctopcl;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineController {

    @FXML
    private TextField serverAddress;
    @FXML
    private TextField serverPort;
    @FXML
    private TextField connectionPassword;
    @FXML
    private ChoiceBox<String> protocolType;
    @FXML
    private TextField localAddress;
    @FXML
    private TextField localPort;
    @FXML
    private TextField remotePort;
    @FXML
    private Button connectButton;
    @FXML
    private Button disconnectButton;
    @FXML
    private Label statusLabel;
    
    private Stage stage;
    private Socket serverSocket;
    private ExecutorService executorService;
    private ScheduledExecutorService statusCheckExecutor;
    private boolean isConnected = false;
    
    // 维护外部连接的映射：clientId -> localSocket
    private Map<String, Socket> externalConnections = new ConcurrentHashMap<>();
    
    @FXML
    public void initialize() {
        // 加载配置
        OnlineConfig.load();
        
        // 初始化协议类型选择
        protocolType.getItems().addAll("TCP", "UDP");
        
        // 从配置中加载设置
        serverAddress.setText(OnlineConfig.getString("server.address", ""));
        serverPort.setText(OnlineConfig.getString("server.port", ""));
        connectionPassword.setText(OnlineConfig.getString("connection.password", ""));
        protocolType.setValue(OnlineConfig.getString("protocol.type", "TCP"));
        localAddress.setText(OnlineConfig.getString("local.address", "127.0.0.1"));
        localPort.setText(OnlineConfig.getString("local.port", ""));
        remotePort.setText(OnlineConfig.getString("remote.port", ""));
        
        // 初始化按钮状态
        disconnectButton.setDisable(true);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        // 添加窗口关闭事件监听器，确保安全关闭连接
        stage.setOnCloseRequest(event -> {
            onClose();
        });
    }

    @FXML
    protected void onConnect() {
        try {
            String address = serverAddress.getText();
            int port = Integer.parseInt(serverPort.getText());
            String password = connectionPassword.getText();
            String protocol = protocolType.getValue();
            String localAddr = localAddress.getText();
            int localPortNum = Integer.parseInt(localPort.getText());
            String remotePortStr = remotePort.getText();
            
            // 验证输入
            if (address.isEmpty() || port <= 0 || port > 65535) {
                showError("错误", "服务器地址或端口无效");
                return;
            }
            
            if (localAddr.isEmpty() || localPortNum <= 0 || localPortNum > 65535) {
                showError("错误", "本地地址或端口无效");
                return;
            }
            
            // 开始连接
            statusLabel.setText("连接中...");
            
            executorService = Executors.newCachedThreadPool();
            
            // 连接到公网服务器
            SocketAddress socketAddress = new InetSocketAddress(address, port);
            serverSocket = new Socket();
            // 设置连接超时时间为10秒
            serverSocket.connect(socketAddress, 10000);
            
            // 发送认证信息
            OutputStream out = serverSocket.getOutputStream();
            // 构建认证消息，包含远程端口参数
            String authMessage = "AUTH:" + password + ":" + protocol + ":" + localAddress.getText() + ":" + localPortNum;
            // 如果用户指定了远程端口，添加到认证消息中
            if (!remotePortStr.isEmpty()) {
                authMessage += ":" + remotePortStr;
            }
            out.write((authMessage + "\n").getBytes());
            out.flush();
            
            // 读取认证响应
            InputStream in = serverSocket.getInputStream();
            byte[] buffer = new byte[1024];
            int read = in.read(buffer);
            if (read == -1) {
                throw new IOException("未收到认证响应，连接已关闭");
            }
            String response = new String(buffer, 0, read).trim();
            
            if (response.startsWith("OK")) {
                isConnected = true;
                statusLabel.setText("已连接");
                connectButton.setDisable(true);
                disconnectButton.setDisable(false);
                
                // 保存配置
                saveConfig();
                
                // 启动连接状态检查
                startStatusCheck();
                
                // 开始处理转发
                if (protocol.equals("TCP")) {
                    startTCPForwarding();
                } else {
                    startUDPForwarding();
                }
            } else {
                showError("连接失败", "认证失败: " + response);
                serverSocket.close();
                statusLabel.setText("未连接");
            }
            
        } catch (NumberFormatException e) {
            showError("错误", "端口号必须是数字");
        } catch (IOException e) {
            showError("连接失败", "无法连接到服务器: " + e.getMessage());
            statusLabel.setText("未连接");
        }
    }

    @FXML
    protected void onDisconnect() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
            
            if (statusCheckExecutor != null && !statusCheckExecutor.isShutdown()) {
                statusCheckExecutor.shutdown();
            }
            
            isConnected = false;
            statusLabel.setText("未连接");
            connectButton.setDisable(false);
            disconnectButton.setDisable(true);
            
        } catch (IOException e) {
            // 忽略错误
        }
    }

    @FXML
    protected void onClose() {
        onDisconnect();
        if (stage != null) {
            stage.close();
        }
    }

    private void startTCPForwarding() {
        executorService.submit(() -> {
            try {
                InputStream in = serverSocket.getInputStream();
                byte[] buffer = new byte[4096];
                
                while (isConnected && !serverSocket.isClosed()) {
                    // 等待服务器发送数据
                    int read = in.read(buffer);
                    
                    if (read == -1) break;
                    
                    // 尝试将数据解析为字符串，检查是否是命令
                    String request = null;
                    try {
                        request = new String(buffer, 0, read).trim();
                    } catch (Exception e) {
                        // 无法解析为字符串，可能是二进制数据
                    }
                    
                    // 创建一个final副本用于lambda表达式
                    final String finalRequest = request;
                    
                    if (finalRequest != null && finalRequest.startsWith("CONNECT")) {
                        // 解析连接请求
                        String[] parts = finalRequest.split(":");
                        if (parts.length >= 2) {
                            final String clientId = parts[1];
                            
                            // 连接到本地服务
                            try {
                                final Socket localSocket = new Socket(localAddress.getText(), Integer.parseInt(localPort.getText()));
                                
                                // 将连接添加到映射中
                                externalConnections.put(clientId, localSocket);
                                
                                // 通知服务器连接成功
                                OutputStream out = serverSocket.getOutputStream();
                                out.write(("CONNECTED:" + clientId + "\n").getBytes());
                                out.flush();
                                
                                // 连接到本地服务成功
                                
                                // 启动从本地服务到服务端的转发
                                executorService.submit(() -> {
                                    try {
                                        InputStream localIn = localSocket.getInputStream();
                                        OutputStream serverOut = serverSocket.getOutputStream();
                                        
                                        byte[] localBuffer = new byte[4096];
                                        int localRead;
                                        while ((localRead = localIn.read(localBuffer)) != -1) {
                                            serverOut.write(localBuffer, 0, localRead);
                                            serverOut.flush();
                                        }
                                    } catch (IOException e) {
                                        // 忽略错误
                                    } finally {
                                        try {
                                            if (!localSocket.isClosed()) {
                                                localSocket.close();
                                            }
                                            // 从映射中移除连接
                                            externalConnections.entrySet().removeIf(entry -> entry.getValue() == localSocket);
                                        } catch (IOException e) {
                                            // 忽略
                                        }
                                    }
                                });
                                
                            } catch (IOException e) {
                                OutputStream out = serverSocket.getOutputStream();
                                out.write(("CONNECT_ERROR:" + clientId + ":" + e.getMessage() + "\n").getBytes());
                                out.flush();
                                // 连接本地服务失败
                            }
                        }
                    } else if (finalRequest != null && finalRequest.equals("PONG")) {
                        // 处理服务端的心跳响应
                        // 这里不需要做特殊处理，心跳包的主要作用是保持连接活跃
                    } else if (finalRequest != null && finalRequest.startsWith("ERROR")) {
                        // 处理服务端的错误消息
                        System.out.println("Server error: " + finalRequest);
                        Platform.runLater(() -> {
                            showError("服务端错误", finalRequest.substring(6));
                            statusLabel.setText("连接异常");
                        });
                    } else {
                        // 处理来自服务端的数据（可能是外部用户的连接数据或二进制数据）
                        // 由于服务端会为每个外部连接创建一个独立的数据流
                        // 我们需要将这些数据转发到对应的本地服务
                        
                        // 遍历所有外部连接，将数据转发到每个连接
                        // 注意：这是一个简化的实现，实际的实现应该根据连接ID来转发
                        for (Map.Entry<String, Socket> entry : externalConnections.entrySet()) {
                            String clientId = entry.getKey();
                            Socket localSocket = entry.getValue();
                            
                            try {
                                OutputStream localOut = localSocket.getOutputStream();
                                localOut.write(buffer, 0, read);
                                localOut.flush();
                            } catch (IOException e) {
                                // 连接失败，移除对应的映射
                                externalConnections.remove(clientId);
                                try {
                                    localSocket.close();
                                } catch (IOException ex) {
                                    // 忽略
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                if (isConnected) {
                    Platform.runLater(() -> statusLabel.setText("连接断开"));
                }
            }
        });
    }

    private void startTCPForwardingThread(Socket serverSock, Socket localSock, String clientId) {
        // 注意：不能直接使用serverSock进行数据读写，因为它会与主startTCPForwarding线程的读取操作冲突
        // 正确的做法是：
        // 1. 服务端创建一个新的Socket与外部用户连接
        // 2. 服务端与客户端之间建立一个新的数据流通道
        // 3. 客户端与本地服务之间建立数据转发
        
        // 但为了简化实现，我们可以使用以下方法：
        // 1. 客户端连接到本地服务
        // 2. 客户端通知服务端连接成功
        // 3. 服务端和客户端之间通过现有的连接进行数据转发
        
        // 启动从本地服务到服务端的转发
        executorService.submit(() -> {
            try {
                InputStream localIn = localSock.getInputStream();
                OutputStream serverOut = serverSock.getOutputStream();
                
                byte[] buffer = new byte[4096];
                int read;
                while ((read = localIn.read(buffer)) != -1) {
                    // 发送本地服务的数据到服务端
                    serverOut.write(buffer, 0, read);
                    serverOut.flush();
                }
            } catch (IOException e) {
                // 忽略错误
            } finally {
                try {
                    localSock.close();
                } catch (IOException e) {
                    // 忽略
                }
            }
        });
    }

    private void startUDPForwarding() {
        executorService.submit(() -> {
            try {
                DatagramSocket udpSocket = new DatagramSocket();
                
                while (isConnected && !serverSocket.isClosed()) {
                    // 处理UDP数据包
                    byte[] buffer = new byte[4096];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    
                    // 从服务器接收UDP数据
                    InputStream in = serverSocket.getInputStream();
                    int read = in.read(buffer);
                    if (read == -1) break;
                    
                    String data = new String(buffer, 0, read).trim();
                    if (data.startsWith("UDP_DATA")) {
                        // 解析UDP数据
                        String[] parts = data.split(":", 3);
                        if (parts.length >= 3) {
                            String clientInfo = parts[1];
                            byte[] udpData = parts[2].getBytes();
                            
                            // 发送到本地服务
                            DatagramSocket localUdpSocket = new DatagramSocket();
                            InetAddress localAddr = InetAddress.getByName(localAddress.getText());
                            DatagramPacket localPacket = new DatagramPacket(
                                    udpData, udpData.length,
                                    localAddr, Integer.parseInt(localPort.getText())
                            );
                            localUdpSocket.send(localPacket);
                            
                            // 接收本地服务的响应
                            byte[] responseBuffer = new byte[4096];
                            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                            localUdpSocket.setSoTimeout(5000);
                            
                            try {
                                localUdpSocket.receive(responsePacket);
                                byte[] responseData = new byte[responsePacket.getLength()];
                                System.arraycopy(responseBuffer, 0, responseData, 0, responsePacket.getLength());
                                
                                // 发送响应到服务器
                                OutputStream out = serverSocket.getOutputStream();
                                String response = "UDP_RESPONSE:" + clientInfo + ":" + new String(responseData);
                                out.write((response + "\n").getBytes());
                                out.flush();
                            } catch (SocketTimeoutException e) {
                                // 忽略超时错误
                            }
                            
                            localUdpSocket.close();
                        }
                    }
                }
                
                udpSocket.close();
            } catch (IOException e) {
                if (isConnected) {
                    Platform.runLater(() -> statusLabel.setText("连接断开"));
                }
            }
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * 启动连接状态检查
     */
    private void startStatusCheck() {
        statusCheckExecutor = Executors.newSingleThreadScheduledExecutor();
        // 每10秒检查一次连接状态
        statusCheckExecutor.scheduleAtFixedRate(() -> {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    // 尝试发送一个空的心跳包来检查连接
                    OutputStream out = serverSocket.getOutputStream();
                    out.write("PING\n".getBytes());
                    out.flush();
                    
                    // 注意：不再在这里读取响应，避免与startTCPForwarding线程冲突
                    // 心跳包的响应会被startTCPForwarding线程读取并处理
                    // 这里只负责发送心跳包，确保连接保持活跃
                } else {
                    throw new IOException("连接已关闭");
                }
            } catch (Exception e) {
                // 连接异常，更新状态
                if (isConnected) {
                    isConnected = false;
                    Platform.runLater(() -> {
                        statusLabel.setText("连接断开");
                        connectButton.setDisable(false);
                        disconnectButton.setDisable(true);
                        
                        // 关闭相关资源
                        try {
                            if (serverSocket != null && !serverSocket.isClosed()) {
                                serverSocket.close();
                            }
                            if (executorService != null && !executorService.isShutdown()) {
                                executorService.shutdown();
                            }
                        } catch (IOException ex) {
                            // 忽略错误
                        }
                    });
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    /**
     * 保存配置
     */
    private void saveConfig() {
        OnlineConfig.setString("server.address", serverAddress.getText());
        OnlineConfig.setString("server.port", serverPort.getText());
        OnlineConfig.setString("connection.password", connectionPassword.getText());
        OnlineConfig.setString("protocol.type", protocolType.getValue());
        OnlineConfig.setString("local.address", localAddress.getText());
        OnlineConfig.setString("local.port", localPort.getText());
        OnlineConfig.setString("remote.port", remotePort.getText());
        OnlineConfig.save();
    }
}
