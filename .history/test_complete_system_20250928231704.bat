@echo off
echo ========================================
echo    TEST DISTRIBUTED FILE DOWNLOADER
echo ========================================
echo.

echo [1/8] Dang build project...
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)

call mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
echo ✓ Build thanh cong!

echo.
echo [2/8] Dang khoi dong Master Server (Port 12345)...
start "Master Server" java -cp "target/classes;target/dependency/*" com.example.Main server 12345

echo [3/8] Dang khoi dong Slave Server 1 (Port 12346)...
start "Slave Server 1" java -cp "target/classes;target/dependency/*" com.example.Main server 12346

echo [4/8] Dang khoi dong Slave Server 2 (Port 12347)...
start "Slave Server 2" java -cp "target/classes;target/dependency/*" com.example.Main server 12347

echo [5/8] Cho servers khoi dong...
timeout /t 5 /nobreak >nul

echo [6/8] Test ket noi den tat ca servers...
echo Testing Master Server (12345)...
java -cp "target/classes;target/dependency/*" com.example.client.Client 12345
if %errorlevel% neq 0 (
    echo ERROR: Master Server khong hoat dong!
    pause
    exit /b 1
)

echo Testing Slave Server 1 (12346)...
java -cp "target/classes;target/dependency/*" com.example.client.Client 12346
if %errorlevel% neq 0 (
    echo ERROR: Slave Server 1 khong hoat dong!
    pause
    exit /b 1
)

echo Testing Slave Server 2 (12347)...
java -cp "target/classes;target/dependency/*" com.example.client.Client 12347
if %errorlevel% neq 0 (
    echo ERROR: Slave Server 2 khong hoat dong!
    pause
    exit /b 1
)

echo ✓ Ca 3 servers hoat dong tot!

echo.
echo [7/8] Test download cac file...
echo Testing test.txt...
java -cp "target/classes;target/dependency/*" com.example.client.ConcurrentDownloadClient test.txt
if %errorlevel% neq 0 (
    echo ERROR: Download test.txt failed!
    pause
    exit /b 1
)

echo Testing sample.txt...
java -cp "target/classes;target/dependency/*" com.example.client.ConcurrentDownloadClient sample.txt
if %errorlevel% neq 0 (
    echo ERROR: Download sample.txt failed!
    pause
    exit /b 1
)

echo ✓ Download thanh cong!

echo.
echo [8/8] Mo Client GUI...
start "Client GUI" java -cp "target/classes;target/dependency/*" com.example.Main

echo.
echo ========================================
echo    TEST HOAN TAT - TAT CA OK!
echo ========================================
echo.
echo ========================================
echo    HUONG DAN SU DUNG GUI
echo ========================================
echo 1. Nhan "Lay Danh Sach File" de xem files co san
echo 2. Chon file va nhan "Tai File Duoc Chon" 
echo 3. Hoac nhan "Tai File Mac Dinh" de tai test.txt
echo 4. Nhan "Xem File" de xem noi dung file da tai
echo 5. Nhan "Xuat File" de luu file ra vi tri khac
echo 6. Test cac file lon: large_document.txt, system_admin.sh, DistributedComputingExample.java
echo.
echo ========================================
echo    DEMO FEATURES
echo ========================================
echo ✓ 3 servers hoat dong (Master + 2 Slaves)
echo ✓ Download song song tu nhieu server
echo ✓ Resume capability
echo ✓ Hash verification (SHA-256)
echo ✓ File viewing va export
echo ✓ Multi-file support (5 files)
echo ✓ Error handling va retry
echo.
echo Nhan phim bat ky de thoat...
pause >nul
