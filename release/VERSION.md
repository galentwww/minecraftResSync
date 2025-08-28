# Minecraft Resource Sync Tool - 发布信息

## 版本详情
- **版本号**: 1.0.0
- **发布日期**: 2025-08-28
- **构建时间**: 15:03 CST

## 构建环境
- **JDK版本**: Java 11+
- **构建工具**: Apache Maven 3.9.11
- **主要依赖**:
  - Gson 2.10.1 (JSON解析)
  - FlatLaf 3.2.5 (现代UI主题)
  - FlatLaf Extras 3.2.5 (UI增强组件)

## 文件清单
- `minecraftResSync.jar` - 主程序文件 (包含所有依赖)
- `modlist.json` - 示例数据文件 (本地备份)
- `README.md` - 完整使用说明
- `start.sh` - Linux/macOS 启动脚本
- `start.bat` - Windows 启动脚本
- `VERSION.md` - 版本信息文件

## 功能特性
✅ 现代化GUI界面 (FlatLaf深色主题)
✅ 三种启动模式 (GUI/GUI+自动/命令行)
✅ 7阶段工作流程管理
✅ 智能文件管理 (Hash验证)
✅ 可选mod选择弹窗 (左右分屏+说明)
✅ 实时进度监控
✅ 完整的中文本地化
✅ 网络异常时本地备份支持
✅ 非侵入式文件管理
✅ 命令行参数时URL锁定

## 兼容性
- **操作系统**: Windows 10+, macOS 10.14+, Linux (主流发行版)
- **Java版本**: Java 11 或更高版本
- **Minecraft版本**: 1.20.1+ (根据mod列表而定)

## 安全特性
- 基于MD5的文件完整性验证
- 只处理匹配的mod文件，保留用户自定义内容
- 本地备份机制，离线可用
- 无需管理员权限

## 技术架构
- **前端**: Java Swing + FlatLaf
- **网络**: Java HttpURLConnection
- **数据**: JSON (Gson解析)
- **构建**: Maven + Shade Plugin (Fat JAR)

## 更新日志

### v1.0.0 (2025-08-28)
- ✨ 初始版本发布
- 🎨 现代化GUI界面设计
- 🚀 完整工作流程实现
- 🔧 可选mod选择功能
- 📊 实时统计和进度显示
- 🌐 完整中文本地化
- 🛡️ 文件安全管理机制