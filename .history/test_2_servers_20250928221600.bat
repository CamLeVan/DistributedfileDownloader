@echo off
echo ========================================
echo    TEST 2 SERVERS TREN 1 MAY
echo ========================================
echo.

echo [1/6] Dang build project...
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)

call mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
echo ✓ Build thanh cong!

echo.
echo [2/6] Dang khoi dong Master Server (Port 12345)...
start "Master Server" java -cp "target/classes;target/dependency/*" com.example.Main server 12345

echo [3/6] Dang khoi dong Slave Server (Port 12346)...
start "Slave Server" java -cp "target/classes;target/dependency/*" com.example.Main server 12346

echo [4/6] Cho servers khoi dong...
timeout /t 5 /nobreak >nul

echo [5/6] Test ket noi den servers...
echo Testing Master Server (12345)...
java -cp "target/classes;target/dependency/*" com.example.client.Client 12345
if %errorlevel% neq 0 (
    echo ERROR: Master Server khong hoat dong!
    pause
    exit /b 1
)

echo Testing Slave Server (12346)...
java -cp "target/classes;target/dependency/*" com.example.client.Client 12346
if %errorlevel% neq 0 (
    echo ERROR: Slave Server khong hoat dong!
    pause
    exit /b 1
)

echo ✓ Ca 2 servers hoat dong tot!

echo.
echo [6/6] Test download file...
java -cp "target/classes;target/dependency/*" com.example.client.ConcurrentDownloadClient test.txt
if %errorlevel% neq 0 (
    echo ERROR: Download failed!
    pause
    exit /b 1
)

echo ✓ Download thanh cong!

echo.
echo ========================================
echo    TEST HOAN TAT - TAT CA OK!
echo ========================================
echo.
echo Dang mo Client GUI...
start "Client GUI" java -cp "target/classes;target/dependency/*" com.example.Main

echo.
echo ========================================
echo    HUONG DAN SU DUNG GUI
echo ========================================
echo 1. Nhan "Lay Danh Sach File" de xem files co san
echo 2. Chon file va nhan "Tai File Duoc Chon" 
echo 3. Hoac nhan "Tai File Mac Dinh" de tai test.txt
echo 4. Nhan "Xem File" de xem noi dung file da tai
echo 5. Nhan "Xuat File" de luu file ra vi tri khac
echo.
echo Nhan phim bat ky de thoat...
pause >nul
