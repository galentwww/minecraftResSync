# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java tool `minecraftResSync.jar` that synchronizes Minecraft mod resources by fetching mod lists from remote APIs and displaying them with local fallback support. The tool is designed to run before Minecraft starts to prepare the mod environment.

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

#### GUI Mode (Default)
```bash
java -jar target/minecraftResSync.jar
# or explicitly:
java -jar target/minecraftResSync.jar --gui
```

#### Command Line Mode
```bash
java -jar target/minecraftResSync.jar <api-endpoint-url>

# Example:
java -jar target/minecraftResSync.jar https://api.galentwww.cn/items/modlist
```

## Architecture

### Core Components
- **Main.java**: Entry point with dual mode support - GUI mode (default) or command line mode
- **MinecraftResSyncGUI.java**: Modern Swing GUI with FlatLaf dark theme, table display, and statistics panel
- **ArgumentParser**: Validates command line arguments and URL format  
- **HttpClient**: Handles HTTP GET requests with proper timeout and headers
- **JsonParser**: Parses JSON using Gson library, supports both string and file input
- **ModInfo**: Data model for individual mod information (name, ID, subject, required status)
- **ModListResponse**: Container for the full mod list API response

### Data Flow
1. Parse command line arguments for API endpoint URL
2. Attempt to fetch mod list from remote API using HttpClient
3. Parse JSON response using JsonParser into ModListResponse/ModInfo objects
4. If remote fails, fallback to local `modlist.json` file
5. Display formatted mod list with statistics grouped by subject and required status

### Key Features
- Remote API fetching with local file fallback
- JSON parsing and data modeling with Gson
- Statistical analysis by mod subject/category
- Required vs optional mod tracking
- Hardcoded local file path: `/Users/galentwww/IdeaProjects/minecraftResSync/modlist.json`

## Dependencies

- **Java 11+** (configured in pom.xml)
- **Gson 2.10.1** for JSON parsing
- **FlatLaf 3.2.5** for modern GUI look and feel
- **FlatLaf Extras 3.2.5** for enhanced GUI components  
- **Maven Shade Plugin** for creating fat JAR with dependencies
- Standard Java HTTP client (no external HTTP library)

## File Structure

- `src/main/java/Main.java` - Application entry point with dual mode support
- `src/main/java/com/minecraft/sync/MinecraftResSyncGUI.java` - Modern GUI interface
- `src/main/java/com/minecraft/sync/` - Core package with all utility classes
- `lib/gson-2.10.1.jar` - Gson dependency (for manual builds)
- `modlist.json` - Local mod list data for fallback
- `target/minecraftResSync.jar` - Built executable JAR with all dependencies
- `pom.xml` - Maven configuration with Shade plugin for fat JAR
- `build.sh` - Automated build script