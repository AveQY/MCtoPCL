# MCtoPCL 服务端

这是MCtoPCL项目的服务端部分，用于在内网穿透场景中作为公网服务器，接收客户端连接并转发数据到内网服务。

## 功能特点

- 支持TCP协议转发（适用于MC服务器等）
- 支持UDP协议转发
- 密码认证机制
- 多客户端连接支持
- 详细的日志记录
- 可配置的服务器参数

## 环境要求

- JDK 20 或更高版本
- Linux 操作系统
- Maven 3.6 或更高版本（用于构建）

## 构建步骤

1. 进入服务端目录
   ```bash
   cd service-side
   ```

2. 构建项目
   ```bash
   mvn clean package
   ```

3. 构建成功后，可执行文件会生成在 `target` 目录中

## 配置说明

服务端通过 `server.properties` 文件进行配置，主要配置项包括：

- `server.port` - 服务器监听端口（默认：8080）
- `server.password` - 连接密码（请务必修改为安全的密码）
- `server.max_connections` - 最大连接数（默认：100）
- `server.timeout` - 连接超时时间（毫秒，默认：30000）
- `forward.buffer_size` - 缓冲区大小（默认：4096）
- `log.level` - 日志级别（debug, info, warn, error，默认：info）

## 运行方法

### 方法一：使用启动脚本

1. 赋予脚本执行权限
   ```bash
   chmod +x start.sh
   ```

2. 运行脚本
   ```bash
   ./start.sh
   ```

### 方法二：直接运行jar文件

```bash
java -jar target/server-side-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 安全建议

1. **修改默认密码**：在 `server.properties` 文件中修改 `server.password` 为安全的密码
2. **防火墙设置**：确保服务器防火墙允许配置的端口访问
3. **定期更新**：定期检查并更新服务端代码
4. **限制访问**：如可能，限制只有特定IP可以访问服务器

## 客户端配置

客户端（MCtoPCL应用）需要配置以下信息：

- **服务器地址**：公网服务器的IP地址或域名
- **服务器端口**：与 `server.properties` 中的 `server.port` 一致
- **连接密码**：与 `server.properties` 中的 `server.password` 一致
- **本地地址**：内网服务的地址（通常为 localhost）
- **本地端口**：内网服务的端口（如MC服务器默认25565）

## 故障排查

### 常见问题

1. **连接失败**
   - 检查服务器是否运行
   - 检查网络连接是否正常
   - 检查防火墙设置
   - 检查密码是否正确

2. **转发失败**
   - 检查本地服务是否运行
   - 检查本地端口是否正确
   - 检查服务器是否有权限访问本地服务

3. **性能问题**
   - 考虑增加服务器资源
   - 调整缓冲区大小
   - 限制并发连接数

### 日志查看

服务端会输出详细的日志信息，可以通过日志了解连接状态和错误原因。

## 许可证

MIT License
