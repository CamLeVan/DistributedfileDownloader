@echo off
echo ========================================
echo    KHOI DONG DISTRIBUTED FILE SERVER
echo ========================================
echo.

echo Dang khoi dong Master Server (Port 12345)...
start "Master Server" java -cp "target/classes;target/dependency/*" com.example.Main server 12345

timeout /t 2

echo Dang khoi dong Slave Server 1 (Port 12346)...
start "Slave Server 1" java -cp "target/classes;target/dependency/*" com.example.Main server 12346

timeout /t 2

echo Dang khoi dong Slave Server 2 (Port 12347)...
start "Slave Server 2" java -cp "target/classes;target/dependency/*" com.example.Main server 12347

timeout /t 3

echo Dang khoi dong Client GUI...
start "Client GUI" java -cp "target/classes;target/dependency/*" com.example.Main

echo.
echo ========================================
echo    TAT CA SERVER DA KHOI DONG
echo ========================================
echo Master Server: localhost:12345
echo Slave Server 1: localhost:12346  
echo Slave Server 2: localhost:12347
echo Client GUI: Da mo
echo.
pause
