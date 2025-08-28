# 🎮 Minecraft Resource Sync Tool

[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)](https://github.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

一个现代化的Minecraft资源同步工具，专为模组整合包管理而设计。支持从远程API自动获取和管理mod、资源包、光影包和配置文件，提供完整的图形化界面和命令行操作。

## 🌟 项目特色

### 🔧 技术架构
- **纯Java实现**：基于Java 11+，跨平台兼容
- **现代化UI**：采用FlatLaf主题，提供优雅的深色界面
- **模块化设计**：清晰的代码分层和职责分离
- **RESTful API集成**：支持远程数据源同步
- **智能缓存**：本地备份机制，确保离线可用性

## 🚀 快速开始

### 🎯 安装方式

#### 方式1：直接使用发行版
```bash
# 下载最新版本
wget https://github.com/galentwww/minecraftResSync/releases/latest/download/minecraftResSync.jar

# 直接运行
java -jar minecraftResSync.jar
```

#### 方式2：源码编译
```bash
# 克隆项目
git clone https://github.com/your-repo/minecraftResSync.git
cd minecraftResSync

# 一键构建
chmod +x build.sh
./build.sh

# 运行程序
java -jar target/minecraftResSync.jar
```

## 🏗️ 项目结构

```
minecraftResSync/
├── 📁 src/main/java/                 # 源代码目录
│   ├── Main.java                     # 程序入口
│   └── com/minecraft/sync/           # 核心模块
│       ├── MinecraftResSyncGUI.java  # 图形界面
│       ├── JsonParser.java          # JSON解析
│       ├── HttpClient.java          # 网络请求
│       ├── FileDownloader.java      # 文件下载
│       ├── UpdateChecker.java       # 更新检查
│       ├── ArgumentParser.java      # 参数解析
│       ├── ModInfo.java             # 数据模型
│       └── ModListResponse.java     # 响应封装
├── 📁 release/                       # 发行文件
│   ├── minecraftResSync.jar         # 可执行程序
│   ├── README.md                    # 使用说明
│   ├── start.bat                    # Windows启动脚本
│   └── start.sh                     # Linux/macOS启动脚本
├── 📁 mods/                          # 模组存储目录
├── 📁 resourcepacks/                # 资源包存储目录
├── 📁 shaderpacks/                  # 光影包存储目录
├── 📁 config/                       # 配置文件目录
├── pom.xml                          # Maven配置
├── build.sh                         # 构建脚本
└── modlist.json                     # 本地模组列表
```

## 🎮 使用指南

### 📋 图形界面操作

1. **启动程序**：双击或命令行启动
2. **获取数据**：自动从API获取最新模组列表
3. **浏览模组**：按分类查看可用模组
4. **选择下载**：勾选需要的模组
5. **执行同步**：一键下载并安装
6. **验证完成**：Hash验证确保完整性

### ⚙️ 命令行操作

```bash
# 基础使用
java -jar minecraftResSync.jar --cli https://api.example.com/modlist

# 带参数使用
java -jar minecraftResSync.jar https://api.example.com/modlist

# 查看帮助
java -jar minecraftResSync.jar --help
```

#### 日志调试
```bash
# 启用详细日志
java -jar minecraftResSync.jar --debug

# 查看版本信息
java -jar minecraftResSync.jar --version
```

## 🛠️ 开发指南

### 🏗️ 环境搭建

```bash
# 1. 克隆项目
git clone https://github.com/your-repo/minecraftResSync.git

# 2. 安装依赖
mvn clean install

# 3. 运行测试
mvn test

# 4. 构建发行版
mvn package
```

### 📝 代码规范

#### 包结构
```
com.minecraft.sync/
├── model/           # 数据模型
├── network/         # 网络通信
├── ui/              # 用户界面
├── util/            # 工具类
└── exception/       # 异常处理
```
## 如何托管自己的 MOD？

构建一个远程 Modlist，我使用了 Directus，你也可以使用你习惯的方式，或者直接手动编写

```json
{
  "data": [
    {
      "catelog": "mods",
      "description": null,
      "friendly_name": "AppleSkin",
      "hash": "dae63cea9c951dda542a4005ceef3953",
      "id": 71,
      "is_require": true,
      "raw_name": "appleskin-forge-mc1.20.1-2.5.1.jar",
      "res": "https://cdn.modrinth.com/data/EsAfCjCV/versions/XdXDExVF/appleskin-forge-mc1.20.1-2.5.1.jar",
      "subject": "enhance"
    },]
}
```
工具会按照先必需的模组，再可选的模组，最后是资源包、光影包的顺序进行下载，其中必需的模组会先下载 subject 为 libs 的模组，防止网络出现问题时先下载功能性模组导致缺失前置的问题。