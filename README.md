# Minecraft Resource Sync Tool

一个Java工具程序，用于在Minecraft主程序启动前同步mod资源列表。

## 功能特性

- ✅ 通过命令行参数接收远程API端点URL
- ✅ 读取本地 `/Users/galentwww/IdeaProjects/minecraftResSync/modlist.json` 中的资源列表数据
- ✅ 在控制台显示获取到的资源列表
- ✅ 按分类统计mod数量
- ✅ 显示必需mod的统计信息

## 使用方法

### 运行程序

```bash
java -jar minecraftResSync.jar <api-endpoint-url>
```

### 示例

```bash
java -jar minecraftResSync.jar https://api.galentwww.cn/items/modlist
```

### 输出示例

```
=== Minecraft Resource Sync Tool ===
Starting resource synchronization...

API Endpoint: https://api.galentwww.cn/items/modlist

--- Reading local modlist.json ---
Found 69 mods:
================================================================================
  1. Mod: AppleSkin (ID: 71) - enhance [Required] - appleskin-forge-mc1.20.1-2.5.1.jar
  2. Mod: BetterF3 (ID: 72) - enhance [Required] - BetterF3-7.0.2-Forge-1.20.1.jar
  ...
================================================================================

--- Summary by Subject ---
enhance        : 24 mods
gamemode       : 16 mods
beautify       : 13 mods
libs           : 11 mods
others         : 4 mods

Required mods: 63/69

--- Remote API fetching (TODO) ---
Remote API integration will be implemented in the next phase

=== Resource sync completed ===
```

## 项目结构

```
minecraftResSync/
├── src/
│   ├── Main.java                      # 主程序入口
│   └── com/minecraft/sync/
│       ├── ModInfo.java              # Mod信息数据模型
│       ├── ModListResponse.java      # JSON响应数据模型
│       ├── JsonParser.java          # JSON解析工具
│       ├── HttpClient.java          # HTTP客户端工具
│       └── ArgumentParser.java      # 命令行参数解析
├── lib/
│   └── gson-2.10.1.jar              # GSON依赖库
├── target/
│   ├── classes/                     # 编译后的类文件
│   └── minecraftResSync.jar         # 可执行JAR文件
├── modlist.json                     # 本地mod列表数据
├── pom.xml                          # Maven配置文件
└── build.sh                         # 构建脚本
```

## 开发阶段

### 已完成 ✅
- 项目初始化
- 基本框架搭建
- 参数解析功能
- JSON解析功能
- 控制台输出功能
- 本地文件读取

### 待实现 🚧
- 远程API数据获取
- 文件下载功能
- 版本检查与更新
- 错误处理和重试机制

## 构建说明

### 手动构建

1. 下载GSON库：
   ```bash
   curl -L -o lib/gson-2.10.1.jar https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar
   ```

2. 编译Java文件：
   ```bash
   find src -name "*.java" | xargs javac -cp "lib/gson-2.10.1.jar" -d target/classes
   ```

3. 创建JAR文件：
   ```bash
   cd target && jar cfm minecraftResSync.jar MANIFEST.MF -C classes . .
   ```

### 使用Maven构建（需要安装Maven）

```bash
mvn clean compile package
```

## 技术栈

- Java 11+
- Gson 2.10.1 (JSON处理)
- 标准Java HTTP客户端
- 命令行参数解析

## 注意事项

- 程序需要在Minecraft主程序启动前运行
- 确保modlist.json文件路径正确
- 远程API端点需要返回符合格式的JSON数据
- 程序目前仅实现基础功能，文件下载功能将在下一阶段实现