@echo off
chcp 65001 >nul
title Minecraft Resource Sync Tool v1.0.0

echo === Minecraft Resource Sync Tool v1.0.0 ===
echo.

REM 检查Java环境
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: 未找到Java运行环境
    echo 请确保已安装Java 11或更高版本
    echo.
    echo 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo ✅ Java环境检查通过
echo.

REM 检查JAR文件
if not exist "minecraftResSync.jar" (
    echo ❌ 错误: 未找到 minecraftResSync.jar
    echo 请确保文件在当前目录下
    pause
    exit /b 1
)

echo ✅ 程序文件检查通过
echo.

REM 显示启动选项
echo 请选择启动方式:
echo 1. GUI模式 (图形界面)
echo 2. GUI模式 + 自动获取数据
echo 3. 命令行模式
echo 4. 退出
echo.

set /p choice="请输入选项 (1-4): "

if "%choice%"=="1" (
    echo.
    echo 🚀 启动GUI模式...
    java -jar minecraftResSync.jar --gui
) else if "%choice%"=="2" (
    echo.
    set /p api_url="请输入API地址 (回车使用默认): "
    if "%api_url%"=="" set api_url=https://api.galentwww.cn/items/modlist
    echo 🚀 启动GUI模式 (自动获取数据)...
    java -jar minecraftResSync.jar "%api_url%"
) else if "%choice%"=="3" (
    echo.
    set /p api_url="请输入API地址 (回车使用默认): "
    if "%api_url%"=="" set api_url=https://api.galentwww.cn/items/modlist
    echo 🚀 启动命令行模式...
    java -jar minecraftResSync.jar --cli "%api_url%"
) else if "%choice%"=="4" (
    echo 再见！
    exit /b 0
) else (
    echo ❌ 无效选项，请重新运行脚本
    pause
    exit /b 1
)

echo.
echo 程序已退出
pause