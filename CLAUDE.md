# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java tool `minecraftResSync.jar` that synchronizes Minecraft mod resources by fetching mod lists from remote APIs and displaying them with a comprehensive workflow-based GUI. The tool features a step-by-step process for downloading and managing different types of Minecraft resources including mods, resource packs, shaders, and configurations.

## Build and Development Commands

### Maven Build
```bash
mvn clean compile package
```

### Shell Script Build
```bash
./build.sh
```
This script performs a complete build cycle: clean, compile, package, and includes basic testing.

### Manual Build (if Maven unavailable)
```bash
# Download dependencies
curl -L -o lib/gson-2.10.1.jar https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar

# Compile
find src -name "*.java" | xargs javac -cp "lib/gson-2.10.1.jar" -d target/classes

# Package JAR
cd target && jar cfm minecraftResSync.jar MANIFEST.MF -C classes . .
```

### Run the Application

#### GUI Mode with Auto-fetch Workflow
```bash
# Launch GUI with automatic data fetching and workflow support
java -jar target/minecraftResSync.jar https://api.galentwww.cn/items/modlist
```

#### Standard GUI Mode
```bash
# Default GUI (empty form, manual operations)
java -jar target/minecraftResSync.jar
java -jar target/minecraftResSync.jar --gui
```

#### Command Line Mode
```bash
java -jar target/minecraftResSync.jar --cli <api-endpoint-url>

# Example:
java -jar target/minecraftResSync.jar --cli https://api.galentwww.cn/items/modlist
```

## Architecture

### Core Components
- **Main.java**: Entry point with triple mode support - GUI with auto-fetch, GUI mode (default), or command line mode
- **MinecraftResSyncGUI.java**: Advanced Swing GUI with FlatLaf dark theme, workflow management, and step-by-step resource processing
- **ArgumentParser**: Validates command line arguments and URL format  
- **HttpClient**: Handles HTTP GET requests with proper timeout and headers
- **JsonParser**: Parses JSON using Gson library, supports both string and file input
- **ModInfo**: Data model for individual mod information (name, ID, subject, required status, catalog)
- **ModListResponse**: Container for the full mod list API response

### Workflow System
The GUI features a comprehensive 7-stage workflow system:

1. **获取数据 (Fetch Data)**: Downloads mod list from remote API with local fallback
2. **下载必备前置mod (Download Prerequisite Mods)**: Filters and downloads required library mods (catelog=mods, required=true, subject=libs)
3. **下载自定义配置 (Download Configs)**: Downloads configuration files (catelog=config)
4. **下载必备mod (Download Required Mods)**: Downloads all other required mods (catelog=mods, required=true, subject≠libs)
5. **选择并下载可选mod (Select Optional Mods)**: Interactive dialog for selecting optional mods (catelog=mods, required=false)
6. **下载资源包 (Download Resource Packs)**: Downloads required resource packs (catelog=resourcepacks, required=true)
7. **下载光影 (Download Shaders)**: Downloads shader packs (catelog=shaderpacks)

### GUI Features
- **Dual Progress Tracking**: Stage progress bar and individual operation progress bar
- **Tabbed Information Panel**: Statistics and operation logs in separate tabs
- **Interactive MOD Selection**: Dialog for choosing optional mods with select all/none functionality
- **Auto-download Control**: Checkbox to enable/disable automatic downloading
- **Modern Dark Theme**: FlatLaf integration with theme switching capability
- **Real-time Logging**: Timestamped operation logs with automatic scrolling
- **Catalog-based Classification**: Resources grouped by type (mods, resourcepacks, shaderpacks, config)
- **Enhanced Statistics**: Detailed breakdown by catalog and category with required/optional counts

### Data Flow
1. Parse command line arguments for API endpoint URL and mode selection
2. Launch appropriate interface (GUI with auto-fetch, standard GUI, or CLI)
3. Execute workflow stages in sequence:
   - Fetch data from remote API with local fallback
   - Filter and process resources by catalog and requirements
   - Present user choices for optional content
   - Simulate download operations with progress tracking
4. Display formatted resource lists with comprehensive statistics
5. Complete workflow with success confirmation

## Key Features
- **Triple Mode Operation**: GUI with auto-fetch workflow, standard GUI, or command line interface
- **Cross-platform Compatibility**: Optimized for Windows, macOS, and Linux with proper encoding support
- **Step-by-step Workflow**: Guided process through all resource categories
- **Smart Resource Filtering**: Automatic categorization by catalog (mods/resourcepacks/shaderpacks/config)
- **Interactive Selection**: User choice dialogs for optional content
- **Progress Visualization**: Dual progress bars for stage and operation tracking
- **Comprehensive Logging**: Real-time operation logs with timestamps
- **Modern UI**: FlatLaf dark theme with platform-specific font support for Chinese characters
- **Fallback Support**: Local file backup when remote API fails with platform-aware paths
- **UTF-8 Encoding**: Proper Unicode support for Chinese text and file operations

## Dependencies

- **Java 11+** (configured in pom.xml)
- **Gson 2.10.1** for JSON parsing
- **FlatLaf 3.2.5** for modern GUI look and feel
- **FlatLaf Extras 3.2.5** for enhanced GUI components  
- **Maven Shade Plugin** for creating fat JAR with dependencies
- Standard Java HTTP client and Swing components

## File Structure

- `src/main/java/Main.java` - Application entry point with triple mode support
- `src/main/java/com/minecraft/sync/MinecraftResSyncGUI.java` - Advanced workflow GUI with stage management
- `src/main/java/com/minecraft/sync/` - Core package with all utility classes
- `lib/gson-2.10.1.jar` - Gson dependency (for manual builds)
- `modlist.json` - Local mod list data for fallback
- `target/minecraftResSync.jar` - Built executable JAR with all dependencies
- `pom.xml` - Maven configuration with Shade plugin for fat JAR
- `build.sh` - Automated build script