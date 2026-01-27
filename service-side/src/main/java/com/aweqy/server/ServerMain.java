package com.aweqy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {

    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);
    private static final int DEFAULT_PORT = 8080;
    
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private boolean running;
    
    public static void main(String[] args) {
        logger.info("Starting server...");
        ServerMain server = new ServerMain();
        server.start();
    }
    
    public void start() {
        try {
            // 加载配置
            ServerConfig.load();
            
            int port = ServerConfig.getInt("server.port", DEFAULT_PORT);
            
            // 创建服务器套接字
            serverSocket = new ServerSocket(port);
            executorService = Executors.newCachedThreadPool();
            running = true;
            
            logger.info("Server started on port {}", port);
            logger.info("Server password: {}", ServerConfig.getString("server.password", "not_set"));
            
            // 主循环，接受客户端连接
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("New client connected: {}", clientSocket.getInetAddress().getHostAddress());
                    
                    // 为每个客户端创建一个处理器
                    ClientHandler handler = new ClientHandler(clientSocket);
                    executorService.submit(handler);
                    
                } catch (IOException e) {
                    if (!running) {
                        logger.info("Server socket closed");
                    } else {
                        logger.error("Error accepting client connection", e);
                    }
                }
            }
            
        } catch (IOException e) {
            logger.error("Failed to start server", e);
        } finally {
            stop();
        }
    }
    
    public void stop() {
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing server socket", e);
        }
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        logger.info("Server stopped");
    }
}
