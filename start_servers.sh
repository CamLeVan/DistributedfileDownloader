#!/bin/bash

echo "========================================"
echo "   KHOI DONG DISTRIBUTED FILE SERVER"
echo "========================================"
echo

echo "Dang khoi dong Master Server (Port 12345)..."
java -cp "target/classes:target/dependency/*" com.example.Main server 12345 &
MASTER_PID=$!

sleep 2

echo "Dang khoi dong Slave Server 1 (Port 12346)..."
java -cp "target/classes:target/dependency/*" com.example.Main server 12346 &
SLAVE1_PID=$!

sleep 2

echo "Dang khoi dong Slave Server 2 (Port 12347)..."
java -cp "target/classes:target/dependency/*" com.example.Main server 12347 &
SLAVE2_PID=$!

sleep 3

echo "Dang khoi dong Client GUI..."
java -cp "target/classes:target/dependency/*" com.example.Main &
CLIENT_PID=$!

echo
echo "========================================"
echo "   TAT CA SERVER DA KHOI DONG"
echo "========================================"
echo "Master Server: localhost:12345 (PID: $MASTER_PID)"
echo "Slave Server 1: localhost:12346 (PID: $SLAVE1_PID)"
echo "Slave Server 2: localhost:12347 (PID: $SLAVE2_PID)"
echo "Client GUI: Da mo (PID: $CLIENT_PID)"
echo
echo "Nhan Ctrl+C de dung tat ca server"
echo

# Function to cleanup on exit
cleanup() {
    echo "Dang dung tat ca server..."
    kill $MASTER_PID $SLAVE1_PID $SLAVE2_PID $CLIENT_PID 2>/dev/null
    exit 0
}

# Set trap to cleanup on script exit
trap cleanup SIGINT SIGTERM

# Wait for all background processes
wait
