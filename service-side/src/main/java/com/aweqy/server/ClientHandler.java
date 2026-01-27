package com.aweqy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket clientSocket;
    private ExecutorService executorService;
    private boolean authenticated;
    private String clientId;
    private String protocol;
    private String localAddress;
    private int localPort;
    private int remotePort;
    private ServerSocket remoteServerSocket;
    
    // 维护外部连接的映射：clientId -> externalSocket
    private Map<String, Socket> externalConnections = new ConcurrentHashMap<>();
    
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.executorService = Executors.newCachedThreadPool();
        this.authenticated = false;
        this.clientId = "client_" + System.currentTimeMillis();
    }
    
    @Override
    public void run() {
        try {
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();
            
            byte[] buffer = new byte[1024];
            
            // 认证阶段
            if (!authenticate(in, out, buffer)) {
                logger.warn("Client authentication failed: {}", clientSocket.getInetAddress().getHostAddress());
                return;
            }
            
            logger.info("Client authenticated: {}, Protocol: {}, Local Port: {}", clientId, protocol, localPort);
            
            // 处理转发请求
            if (protocol.equals("TCP")) {
                handleTCPRequests(in, out, buffer);
            } else if (protocol.equals("UDP")) {
                handleUDPRequests(in, out, buffer);
            }
            
        } catch (IOException e) {
            logger.error("Error handling client: {}", e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    /**
     * 认证客户端
     */
    private boolean authenticate(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        int read = in.read(buffer);
        if (read == -1) {
            return false;
        }
        
        String message = new String(buffer, 0, read).trim();
        if (!message.startsWith("AUTH:")) {
            out.write("ERROR: Invalid authentication format\n".getBytes());
            out.flush();
            return false;
        }
        
        String[] parts = message.substring(5).split(":");
        if (parts.length < 4) {
            out.write("ERROR: Insufficient authentication data\n".getBytes());
            out.flush();
            return false;
        }
        
        String password = parts[0];
        protocol = parts[1];
        localAddress = parts[2];
        localPort = Integer.parseInt(parts[3]);
        
        // 验证密码
        String serverPassword = ServerConfig.getString("server.password", "default_password");
        if (!password.equals(serverPassword)) {
            out.write("ERROR: Invalid password\n".getBytes());
            out.flush();
            return false;
        }
        
        // 验证协议
        if (!protocol.equals("TCP") && !protocol.equals("UDP")) {
            out.write("ERROR: Invalid protocol\n".getBytes());
            out.flush();
            return false;
        }
        
        // 验证端口
        if (localPort <= 0 || localPort > 65535) {
            out.write("ERROR: Invalid port\n".getBytes());
            out.flush();
            return false;
        }
        
        // 处理远程端口
        if (parts.length > 4) {
            // 客户端指定了远程端口
            try {
                remotePort = Integer.parseInt(parts[4]);
            } catch (NumberFormatException e) {
                out.write("ERROR: Invalid remote port\n".getBytes());
                out.flush();
                return false;
            }
        } else {
            // 自动分配远程端口（从6000开始）
            remotePort = 6000 + (int)(System.currentTimeMillis() % 1000);
        }
        
        // 认证成功
        authenticated = true;
        String response = "OK: Authenticated as " + clientId + " Remote port: " + remotePort;
        out.write((response + "\n").getBytes());
        out.flush();
        
        // 启动远程端口转发服务
        startRemoteForwardingService();
        
        return true;
    }
    
    /**
     * 处理TCP转发请求
     */
    private void handleTCPRequests(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        logger.info("Starting to handle TCP requests");
        while (!clientSocket.isClosed()) {
            try {
                logger.debug("Waiting for client data...");
                int read = in.read(buffer);
                if (read == -1) {
                    logger.info("Client closed the connection, read returned -1");
                    break;
                }
                
                // 尝试将数据解析为字符串，检查是否是命令
                String message = null;
                try {
                    message = new String(buffer, 0, read).trim();
                } catch (Exception e) {
                    // 无法解析为字符串，可能是二进制数据
                }
                
                if (message != null && message.startsWith("CONNECT")) {
                    handleTCPConnectRequest(message, out);
                } else if (message != null && message.equals("PING")) {
                    // 处理心跳请求
                    logger.debug("Received PING, sending PONG");
                    out.write("PONG\n".getBytes());
                    out.flush();
                } else if (message != null && message.startsWith("CONNECTED")) {
                    // 处理客户端连接成功响应
                    logger.debug("Received CONNECTED response: {}", message);
                    // 客户端已成功连接到本地服务
                    // 现在可以开始数据转发
                } else if (message != null && message.startsWith("CONNECT_ERROR")) {
                    // 处理客户端连接错误响应
                    logger.debug("Received CONNECT_ERROR response: {}", message);
                    // 客户端无法连接到本地服务
                    // 可以记录错误信息并关闭相关连接
                } else {
                    // 处理其他数据（来自客户端本地服务的数据或二进制数据）
                    // 将数据转发到所有外部连接
                    // 注意：这是一个简化的实现，实际的实现应该根据客户端ID找到对应的外部连接
                    logger.debug("Received data from client, length: {}", read);
                    
                    // 遍历所有外部连接，将数据转发到每个连接
                    for (Map.Entry<String, Socket> entry : externalConnections.entrySet()) {
                        String externalClientId = entry.getKey();
                        Socket externalSocket = entry.getValue();
                        
                        try {
                            OutputStream externalOut = externalSocket.getOutputStream();
                            externalOut.write(buffer, 0, read);
                            externalOut.flush();
                            logger.debug("Forwarded data to external connection: {}", externalClientId);
                        } catch (IOException e) {
                            logger.error("Error forwarding data to external connection {}: {}", externalClientId, e.getMessage());
                            // 关闭并移除失败的外部连接
                            try {
                                externalSocket.close();
                                externalConnections.remove(externalClientId);
                            } catch (IOException ex) {
                                // 忽略错误
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Error reading from client: {}", e.getMessage());
                break;
            }
        }
        logger.info("Stopped handling TCP requests");
    }
    
    /**
     * 处理TCP连接请求
     */
    private void handleTCPConnectRequest(String message, OutputStream out) {
        String[] parts = message.split(":");
        if (parts.length < 2) {
            sendError(out, "Invalid connect request format");
            return;
        }
        
        String remoteClientId = parts[1];
        logger.info("Received TCP connect request for: {}", remoteClientId);
        
        try {
            // 注意：服务端不能直接连接客户端的本地服务
            // 正确的流程是：
            // 1. 外部用户连接到服务端的转发端口
            // 2. 服务端通知客户端有新的外部连接
            // 3. 客户端收到通知后连接到本地服务
            // 4. 客户端将连接状态通知服务端
            // 5. 服务端和客户端之间建立数据转发
            
            // 这里我们只需要通知客户端有新的外部连接
            // 客户端会在连接到本地服务后发送CONNECTED消息
            logger.info("Notifying client about external connection: {}", remoteClientId);
            
            // 通知客户端有新的外部连接
            out.write(("CONNECT:" + remoteClientId + "\n").getBytes());
            out.flush();
            
        } catch (IOException e) {
            logger.error("Error handling TCP connect request: {}", e.getMessage());
            sendError(out, "Failed to handle connect request: " + e.getMessage());
        }
    }
    
    /**
     * 处理UDP转发请求
     */
    private void handleUDPRequests(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        while (!clientSocket.isClosed()) {
            int read = in.read(buffer);
            if (read == -1) {
                break;
            }
            
            String message = new String(buffer, 0, read).trim();
            if (message.startsWith("UDP_DATA")) {
                handleUDPData(message, out);
            } else if (message.equals("PING")) {
                // 处理心跳请求
                out.write("PONG\n".getBytes());
                out.flush();
            }
        }
    }
    
    /**
     * 处理UDP数据
     */
    private void handleUDPData(String message, OutputStream out) {
        String[] parts = message.split(":", 3);
        if (parts.length < 3) {
            sendError(out, "Invalid UDP data format");
            return;
        }
        
        String remoteClientId = parts[1];
        byte[] udpData = parts[2].getBytes();
        
        logger.debug("Received UDP data from: {}, Length: {}", remoteClientId, udpData.length);
        
        try {
            // 这里应该实现UDP数据转发逻辑
            // 由于UDP是无连接的，需要使用DatagramSocket
            
            // 模拟UDP响应
            String response = "UDP_RESPONSE:" + remoteClientId + ":" + new String(udpData);
            out.write((response + "\n").getBytes());
            out.flush();
            
        } catch (IOException e) {
            logger.error("Failed to handle UDP data: {}", e.getMessage());
            sendError(out, "Failed to handle UDP data: " + e.getMessage());
        }
    }
    
    /**
     * 启动远程端口转发服务
     */
    private void startRemoteForwardingService() {
        executorService.submit(() -> {
            try {
                // 创建远程端口的ServerSocket
                remoteServerSocket = new ServerSocket(remotePort);
                logger.info("Started remote forwarding service on port: {}", remotePort);
                
                // 循环接受外部连接
                while (!clientSocket.isClosed()) {
                    try {
                        // 接受外部连接
                        Socket externalSocket = remoteServerSocket.accept();
                        String externalClientId = "external_" + System.currentTimeMillis();
                        logger.info("Received external connection: {} on port: {}", externalClientId, remotePort);
                        
                        // 通知客户端有新的外部连接
                        OutputStream out = clientSocket.getOutputStream();
                        out.write(("CONNECT:" + externalClientId + "\n").getBytes());
                        out.flush();
                        
                        // 将外部连接添加到映射中
                        externalConnections.put(externalClientId, externalSocket);
                        
                        // 启动双向数据转发
                        // 从外部连接到客户端
                        executorService.submit(() -> {
                            try {
                                InputStream externalIn = externalSocket.getInputStream();
                                OutputStream clientOut = clientSocket.getOutputStream();
                                
                                byte[] buffer = new byte[4096];
                                int read;
                                while ((read = externalIn.read(buffer)) != -1) {
                                    clientOut.write(buffer, 0, read);
                                    clientOut.flush();
                                }
                            } catch (IOException e) {
                                logger.debug("Error forwarding data from external to client: {}", e.getMessage());
                            } finally {
                                try {
                                    externalSocket.close();
                                    // 从映射中移除外部连接
                                    externalConnections.remove(externalClientId);
                                } catch (IOException e) {
                                    // 忽略错误
                                }
                            }
                        });
                        
                        // 注意：从客户端到外部连接的转发会在客户端发送数据时处理
                        
                    } catch (IOException e) {
                        if (!clientSocket.isClosed()) {
                            logger.error("Error accepting external connection: {}", e.getMessage());
                        }
                        break;
                    }
                }
                
            } catch (IOException e) {
                logger.error("Failed to start remote forwarding service on port {}: {}", remotePort, e.getMessage());
                
                // 通知客户端端口转发服务启动失败
                try {
                    OutputStream out = clientSocket.getOutputStream();
                    out.write(("ERROR:Failed to start forwarding service on port " + remotePort + "\n").getBytes());
                    out.flush();
                } catch (IOException ex) {
                    // 忽略错误
                }
            } finally {
                try {
                    if (remoteServerSocket != null && !remoteServerSocket.isClosed()) {
                        remoteServerSocket.close();
                    }
                } catch (IOException e) {
                    logger.error("Error closing remote server socket: {}", e.getMessage());
                }
            }
        });
    }
    
    /**
     * 转发外部连接
     */
    private void forwardExternalConnection(Socket externalSocket, String externalClientId) {
        executorService.submit(() -> {
            try {
                // 注意：服务端不能直接连接客户端的本地服务
                // 正确的流程是：
                // 1. 服务端通知客户端有新的外部连接
                // 2. 客户端收到通知后连接到本地服务
                // 3. 客户端将连接状态通知服务端
                // 4. 服务端和客户端之间建立数据转发
                
                // 这里我们需要等待客户端的CONNECTED响应
                // 但为了简化处理，我们先保持外部连接打开
                // 实际的连接和转发会在客户端响应后处理
                
                // 保持外部连接打开，等待客户端的响应
                logger.info("Waiting for client to connect to local service...");
                
                // 注意：这里的代码需要与客户端的实现配合
                // 客户端会在连接到本地服务后发送CONNECTED消息
                // 服务端在handleTCPRequests方法中处理这个消息
                
            } catch (Exception e) {
                logger.error("Error in forwardExternalConnection: {}", e.getMessage());
                
                // 关闭外部连接
                try {
                    externalSocket.close();
                } catch (IOException ex) {
                    // 忽略错误
                }
            }
        });
    }
    
    /**
     * 启动双向数据转发
     */
    private void startBidirectionalForwarding(Socket sourceSocket, Socket targetSocket, String clientId) {
        // 从源到目标的转发
        executorService.submit(() -> {
            try {
                InputStream sourceIn = sourceSocket.getInputStream();
                OutputStream targetOut = targetSocket.getOutputStream();
                
                byte[] buffer = new byte[4096];
                int read;
                
                while ((read = sourceIn.read(buffer)) != -1) {
                    targetOut.write(buffer, 0, read);
                    targetOut.flush();
                }
                
            } catch (IOException e) {
                logger.debug("Error in data forwarding ({}): {}", clientId, e.getMessage());
            } finally {
                try {
                    sourceSocket.close();
                    targetSocket.close();
                } catch (IOException e) {
                    // 忽略错误
                }
            }
        });
        
        // 从目标到源的转发
        executorService.submit(() -> {
            try {
                InputStream targetIn = targetSocket.getInputStream();
                OutputStream sourceOut = sourceSocket.getOutputStream();
                
                byte[] buffer = new byte[4096];
                int read;
                
                while ((read = targetIn.read(buffer)) != -1) {
                    sourceOut.write(buffer, 0, read);
                    sourceOut.flush();
                }
                
            } catch (IOException e) {
                logger.debug("Error in data forwarding ({}): {}", clientId, e.getMessage());
            } finally {
                try {
                    sourceSocket.close();
                    targetSocket.close();
                } catch (IOException e) {
                    // 忽略错误
                }
            }
        });
    }
    
    /**
     * 启动TCP数据转发
     */
    private void startTCPForwarding(Socket localSocket) {
        executorService.submit(() -> {
            try {
                InputStream clientIn = clientSocket.getInputStream();
                OutputStream localOut = localSocket.getOutputStream();
                
                byte[] buffer = new byte[4096];
                int read;
                
                while ((read = clientIn.read(buffer)) != -1) {
                    localOut.write(buffer, 0, read);
                    localOut.flush();
                }
                
            } catch (IOException e) {
                logger.error("Error in client to local forwarding: {}", e.getMessage());
            } finally {
                try {
                    localSocket.close();
                } catch (IOException e) {
                    // 忽略
                }
            }
        });
        
        executorService.submit(() -> {
            try {
                InputStream localIn = localSocket.getInputStream();
                OutputStream clientOut = clientSocket.getOutputStream();
                
                byte[] buffer = new byte[4096];
                int read;
                
                while ((read = localIn.read(buffer)) != -1) {
                    clientOut.write(buffer, 0, read);
                    clientOut.flush();
                }
                
            } catch (IOException e) {
                logger.error("Error in local to client forwarding: {}", e.getMessage());
            } finally {
                try {
                    localSocket.close();
                } catch (IOException e) {
                    // 忽略
                }
            }
        });
    }
    
    /**
     * 发送错误信息
     */
    private void sendError(OutputStream out, String errorMessage) {
        try {
            out.write(("ERROR:" + errorMessage + "\n").getBytes());
            out.flush();
        } catch (IOException e) {
            logger.error("Failed to send error message: {}", e.getMessage());
        }
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        try {
            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
            
            if (remoteServerSocket != null && !remoteServerSocket.isClosed()) {
                remoteServerSocket.close();
                logger.info("Closed remote forwarding service on port: {}", remotePort);
            }
            
            if (!executorService.isShutdown()) {
                executorService.shutdown();
            }
            
            logger.info("Client connection closed: {}", clientId);
            
        } catch (IOException e) {
            logger.error("Error during cleanup: {}", e.getMessage());
        }
    }
}
