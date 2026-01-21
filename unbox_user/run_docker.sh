#!/bin/bash

# Docker Helper Script for UNBOX-BE

# Function to display menu
show_menu() {
    echo "=============================="
    echo "   Docker Helper Script       "
    echo "=============================="
    echo "1. Start (up --build -d)"
    echo "2. Stop (down)"
    echo "3. Restart (down -> up)"
    echo "4. Logs (logs -f)"
    echo "5. Clean (down -v)"
    echo "0. Exit"
    echo "=============================="
    echo -n "Enter choice [0-5]: "
}

# Function to execute commands
execute_command() {
    case $1 in
        1)
            echo "Starting containers..."
            docker-compose up --build -d
            ;;
        2)
            echo "Stopping containers..."
            docker-compose down
            ;;
        3)
            echo "Restarting containers..."
            docker-compose down
            docker-compose up --build -d
            ;;
        4)
            docker-compose logs -f
            ;;
        5)
            echo "Cleaning up..."
            docker-compose down -v
            ;;
        0)
            echo "Exiting..."
            exit 0
            ;;
        *)
            echo "Invalid option"
            ;;
    esac
}

# Check if arguments are passed
if [ $# -gt 0 ]; then
    case "$1" in
        up) execute_command 1 ;;
        down) execute_command 2 ;;
        restart) execute_command 3 ;;
        logs) execute_command 4 ;;
        clean) execute_command 5 ;;
        *) echo "Usage: $0 {up|down|restart|logs|clean}"; exit 1 ;;
    esac
else
    # Interactive mode
    while true; do
        show_menu
        read choice
        execute_command $choice
        echo ""
    done
fi
