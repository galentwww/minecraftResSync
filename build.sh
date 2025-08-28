#!/bin/bash

echo "=== Building Minecraft Resource Sync Tool ==="

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Maven is not installed. Please install Maven first."
    exit 1
fi

# Clean and compile
echo "Cleaning previous builds..."
mvn clean

echo "Compiling the project..."
mvn compile

echo "Packaging the JAR..."
mvn package

echo "Build completed!"
echo "JAR file created: target/minecraftResSync.jar"

# Test the program with local file
echo ""
echo "=== Testing the program ==="
echo "Testing with local modlist.json..."
echo "Command: java -jar target/minecraftResSync.jar https://api.galentwww.cn/items/modlist"
echo ""