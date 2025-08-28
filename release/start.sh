#!/bin/bash

# Minecraft Resource Sync Tool 启动脚本
# 版本: 1.0.0

echo "=== Minecraft Resource Sync Tool v1.0.0 ==="
echo ""

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到Java运行环境"
    echo "请确保已安装Java 11或更高版本"
    echo ""
    echo "下载地址: https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

# 检查Java版本
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f 2 | sed '/^1\./s///' | cut -d'.' -f 1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo "❌ 错误: Java版本过低 (当前版本: $JAVA_VERSION)"
    echo "请升级到Java 11或更高版本"
    exit 1
fi

echo "✅ Java环境检查通过 (版本: $JAVA_VERSION)"
echo ""

# 检查JAR文件
JAR_FILE="minecraftResSync.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ 错误: 未找到 $JAR_FILE"
    echo "请确保文件在当前目录下"
    exit 1
fi

echo "✅ 程序文件检查通过"
echo ""

# 显示启动选项
echo "请选择启动方式:"
echo "1. GUI模式 (图形界面)"
echo "2. GUI模式 + 自动获取数据"
echo "3. 命令行模式"
echo "4. 退出"
echo ""

# 读取用户选择
read -p "请输入选项 (1-4): " choice

case $choice in
    1)
        echo ""
        echo "🚀 启动GUI模式..."
        java -jar "$JAR_FILE" --gui
        ;;
    2)
        echo ""
        read -p "请输入API地址 (默认: https://api.galentwww.cn/items/modlist): " api_url
        if [ -z "$api_url" ]; then
            api_url="https://api.galentwww.cn/items/modlist"
        fi
        echo "🚀 启动GUI模式 (自动获取数据)..."
        java -jar "$JAR_FILE" "$api_url"
        ;;
    3)
        echo ""
        read -p "请输入API地址 (默认: https://api.galentwww.cn/items/modlist): " api_url
        if [ -z "$api_url" ]; then
            api_url="https://api.galentwww.cn/items/modlist"
        fi
        echo "🚀 启动命令行模式..."
        java -jar "$JAR_FILE" --cli "$api_url"
        ;;
    4)
        echo "再见！"
        exit 0
        ;;
    *)
        echo "❌ 无效选项，请重新运行脚本"
        exit 1
        ;;
esac

echo ""
echo "程序已退出"