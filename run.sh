#!/bin/bash
# Script to load .env file and run the Spring Boot application

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Check if .env file exists
if [ ! -f .env ]; then
    echo "Error: .env file not found in $SCRIPT_DIR"
    exit 1
fi

# Export all variables from .env file (skip comments and empty lines)
export $(grep -v '^#' .env | grep -v '^$' | xargs)

# Run the Spring Boot application
if [ -f "mvnw" ]; then
    ./mvnw spring-boot:run
elif command -v mvn &> /dev/null; then
    mvn spring-boot:run
else
    echo "Error: Maven not found. Please install Maven or use ./mvnw"
    exit 1
fi




