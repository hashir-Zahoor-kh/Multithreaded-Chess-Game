#!/bin/bash

# Build script for Multithreaded Chess Game
# This script helps compile and run the game with JavaFX

echo "Building Multithreaded Chess Game..."

# Check if JavaFX is available
if [ -z "$JAVAFX_PATH" ]; then
    echo "Warning: JAVAFX_PATH not set. Trying to find JavaFX automatically..."
    
    # Try common JavaFX locations
    if [ -d "/usr/share/openjfx/lib" ]; then
        JAVAFX_PATH="/usr/share/openjfx/lib"
    elif [ -d "/opt/openjfx/lib" ]; then
        JAVAFX_PATH="/opt/openjfx/lib"
    elif [ -d "/Library/Java/JavaVirtualMachines/openjfx-*" ]; then
        JAVAFX_PATH=$(ls -d /Library/Java/JavaVirtualMachines/openjfx-* | head -1)/lib
    else
        echo "Error: JavaFX not found. Please install JavaFX or set JAVAFX_PATH"
        echo "For macOS: brew install openjfx"
        echo "For Ubuntu: sudo apt-get install openjfx"
        exit 1
    fi
fi

# Compile the Java files
echo "Compiling Java files..."
javac --module-path "$JAVAFX_PATH" --add-modules javafx.controls,javafx.fxml *.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo ""
    echo "To run the server:"
    echo "  java --module-path \"$JAVAFX_PATH\" --add-modules javafx.controls,javafx.fxml ChessMultithreadedServer"
    echo ""
    echo "To run the client:"
    echo "  java --module-path \"$JAVAFX_PATH\" --add-modules javafx.controls,javafx.fxml ChessGame"
else
    echo "Compilation failed!"
    exit 1
fi
