#!/bin/zsh

BACKEND_BIN_PATH="./backend/src/sysmon"
FRONTEND_JAR_PATH="./frontend/target/"

cd "./backend/src" || { echo "Error: Cannot enter backend directory. Exiting."; exit 1; }

if [ -f "sysmon" ]; then
    echo "Executable found. Running backend..."
    ./sysmon &
else
    echo "Executable not found. Compiling and running..."
    gcc main.c procParser.c terminator.c -o sysmon

    if [ -f "sysmon" ]; then
        ./sysmon &
    else
        echo "Error: Backend compilation failed. Exiting."
        exit 1
    fi
fi

cd - > /dev/null

if [ -d "$FRONTEND_JAR_PATH" ]; then
    echo "Frontend target directory found. Running frontend..."
    cd "./frontend" || { echo "Error: Cannot enter frontend directory. Exiting."; exit 1; }
    mvn javafx:run
else
    echo "Frontend target directory not found. Compiling and running..."
    cd "./frontend" || { echo "Error: Cannot enter frontend directory. Exiting."; exit 1; }
    mvn compile
    mvn javafx:run
fi
