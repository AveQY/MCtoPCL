# MCtoPCL

MC云存档管理工具 - 轻松管理和同步你的Minecraft存档，多人同步联机

## 📁 项目简介

MCtoPCL是一个专为Minecraft玩家设计的存档管理工具，提供本地存档管理、云存储同步和内网穿透联机功能。通过直观的图形界面，你可以管理多个Minecraft存档，实现快速备份、恢复和同步，并且通过远程服务器转发本地MC端口流量实现多人远程同步联机（使用TCP/UDP协议）。

### 核心功能
- **存档管理**：创建、删除、重命名Minecraft存档
- **云存储同步**：支持WebDAV云存储服务，实现存档的云端备份和恢复
- **版本控制**：查看存档的历史版本，随时回滚到之前的状态
- **在线模式**：通过自建服务器转发端口流量机制实现远程联机功能

## 系统要求

### 配置
- **操作系统**：Windows 10+

## 快速开始

### 使用JAR文件
1. **下载JAR文件**
   - 从[发布页面](https://github.com/AveQY/MCtoPCL/releases)下载 `MCtoPCL-*-jar-with-dependencies.jar`

2. **运行JAR**
   ```bash
   java -jar MCtoPCL-*-jar-with-dependencies.jar
   ```

## 使用指南

### 1. 本地存档管理

1. **查看存档列表**
   - 启动应用后，在主界面查看当前所有Minecraft存档
   - 存档会显示名称、大小、修改时间等信息

2. **管理存档**
   - **创建存档**：点击"新建存档"按钮
   - **删除存档**：选择存档后点击"删除"按钮
   - **重命名存档**：右键点击存档选择"重命名"
   - **备份存档**：选择存档后点击"备份"按钮

### 2. 云存储同步

1. **配置云存储**
   - 点击"云存储"按钮打开配置窗口
   - 选择WebDAV服务提供商
   - 输入服务器地址、用户名和密码

2. **同步存档**
   - **上传存档**：选择存档后点击"上传到云端"
   - **下载存档**：点击"从云端下载"按钮，选择要下载的存档

### 3. 联机模式

1. **配置服务器**
   - 在"在线"标签页中，输入服务器地址和端口
   - 点击"连接"按钮连接到服务器

2. **远程联机**
   - 连接成功后，其他玩家可以通过服务器连接到你的游戏
   - 服务器会显示当前在线玩家数量

3. **端口转发**
   - 如需在服务器上设置端口转发，请参考服务端配置说明

## 构建指南

### 前置要求
- **JDK 17** 或更高版本
- **Maven 3.8+**

### 构建步骤

1. **克隆仓库**
   ```bash
   git clone https://github.com/AveQY/MCtoPCL.git
   cd MCtoPCL
   ```

2. **构建项目**
   ```bash
   # 构建JAR文件
   mvn clean package
   
   # 构建包含依赖的JAR文件
   mvn clean package assembly:single
   ```
   
### 构建产物
- **`target/MCtoPCL-*-jar-with-dependencies.jar`** - 包含所有依赖的JAR文件

## 📁 项目结构

```
MCtoPCL/
├── src/                     # 源代码目录
│   ├── main/java/com/aweqy/mctopcl/  # 客户端Java代码
│   └── main/resources/      # 资源文件（FXML、图片等）
├── service-side/            # 服务端代码
│   ├── src/                 # 服务端源代码
│   └── pom.xml              # 服务端Maven配置
├── pom.xml                  # 主Maven配置文件
├── Build-Installer.ps1      # MSI构建脚本
├── installer.wxs            # WiX安装包配置
└── README.md                # 项目说明文档
```

### 核心模块
- **HelloApplication** - 应用程序主入口
- **HelloController** - 主界面控制器
- **CloudStorageConfig** - 云存储配置管理
- **OnlineController** - 在线模式控制器
- **VersionsController** - 版本管理控制器

## 贡献

欢迎贡献代码、报告问题或提出建议！请遵循以下流程：

1. **Fork** 本仓库
2. **创建** 特性分支 (`git checkout -b feature/amazing-feature`)
3. **提交** 更改 (`git commit -m 'Add some amazing feature'`)
4. **推送** 到分支 (`git push origin feature/amazing-feature`)
5. **开启** Pull Request

## 联系方式

- **项目主页**：[https://github.com/AveQY/MCtoPCL](https://github.com/AveQY/MCtoPCL)
- **开发者**：摸鱼校尉（AweQY）
- **反馈**：2049898109@qq.com

---

**享受Minecraft的乐趣，让MCtoPCL为你管理存档！** 🎉
