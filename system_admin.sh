#!/bin/bash
# This is a comprehensive bash script for system administration tasks
# It demonstrates various shell programming concepts and system utilities

echo "Starting system maintenance tasks..."

# Function to check disk usage
check_disk_usage() {
    echo "Checking disk usage..."
    df -h
}

# Function to check memory usage
check_memory_usage() {
    echo "Checking memory usage..."
    free -h
}

# Function to check running processes
check_processes() {
    echo "Checking top processes..."
    ps aux | head -20
}

# Function to backup important files
backup_files() {
    echo "Creating backup..."
    tar -czf backup_$(date +%Y%m%d).tar.gz /etc /home
}

# Function to clean temporary files
clean_temp() {
    echo "Cleaning temporary files..."
    find ity
check_network() {
    echo "Checking network connectivity..."
    ping -c 3 google.com
    netstat -tuln
}

# Function to monitor system logs
monitor_logs() {
    echo "Monitoring system logs..."
    tail -f /var/log/syslog
}

# Function to check security updates
check_security() {
    echo "Checking for security updates..."
    apt list --upgradable
}

# Function to optimize system performance
optimize_system() {
    echo "Optimizing system performance..."
    sync
    echo 3 > /proc/sys/vm/drop_caches
}

# Main execution
main() {
    echo "System Administration Script"
    echo "============================"
    
    check_disk_usage
    check_memory_usage
    check_processes
    check_network
    check_security
    
    read -p "Do you want to perform