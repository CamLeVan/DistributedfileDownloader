# 🌐 HƯỚNG DẪN TRIỂN KHAI ĐA NỀN TẢNG

## 📋 **TỔNG QUAN**

Hướng dẫn chi tiết để triển khai hệ thống Distributed File Downloader trên nhiều máy với các hệ điều hành khác nhau.

## 🏗️ **KIẾN TRÚC TRIỂN KHAI**

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Windows   │    │    Linux    │    │    macOS    │
│   Client    │    │   Master    │    │   Slave 1   │
│             │◄──►│   Server    │◄──►│   Server    │
│             │    │ 192.168.1.2 │    │ 192.168.1.3 │
└─────────────┘    └─────────────┘    └─────────────┘
                           │
                           ▼
                   ┌─────────────┐
                   │   Windows    │
                   │   Slave 2    │
                   │   Server     │
                   │ 192.168.1.4  │
                   └─────────────┘
```

## 🚀 **BƯỚC 1: CHUẨN BỊ GIT REPOSITORY**

### **1.1. Push code lên GitHub/GitLab**
```bash
# Trên máy Windows hiện tại
git add .
git commit -m "Complete distributed file downloader with multi-OS support"
git push origin main
```

### **1.2. Tạo Release với JAR files**
```bash
# Build JAR với dependencies
mvn clean package
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency

# Tạo release package
mkdir release
cp target/dependency/*.jar release/
cp -r target/classes release/
cp start_servers.* release/
cp DEPLOYMENT_GUIDE.md release/
```

## 🖥️ **BƯỚC 2: TRIỂN KHAI TRÊN LINUX (Master Server)**

### **2.1. Cài đặt môi trường**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-11-jdk maven git

# CentOS/RHEL
sudo yum install java-11-openjdk-devel maven git

# Kiểm tra Java
java -version
mvn -version
```

### **2.2. Clone và build project**
```bash
# Clone repository
git clone https://github.com/yourusername/DistributedFileDownloader.git
cd DistributedFileDownloader

# Build project
mvn clean compile
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency
```

### **2.3. Cấu hình Master Server**
```bash
# Sửa application.properties
nano src/main/resources/application.properties
```

```properties
# Master Server Configuration (Linux)
servers=192.168.1.2:12345,192.168.1.3:12346,192.168.1.4:12347
master_server=192.168.1.2:12345
expected_hash=0708271a3ba129cdd31c697d1023bff3b6474cdd08742c533d9541099d07bbb3
file_path=test.txt
db_path=files.db
```

### **2.4. Khởi động Master Server**
```bash
# Cấp quyền thực thi
chmod +x start_servers.sh

# Chạy Master Server
java -cp "target/classes:target/dependency/*" com.example.Main server 12345
```

## 🍎 **BƯỚC 3: TRIỂN KHAI TRÊN macOS (Slave Server 1)**

### **3.1. Cài đặt môi trường**
```bash
# Cài đặt Homebrew (nếu chưa có)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Cài đặt Java và Maven
brew install openjdk@11 maven

# Cấu hình Java
echo 'export PATH="/opt/homebrew/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

### **3.2. Clone và build**
```bash
git clone https://github.com/yourusername/DistributedFileDownloader.git
cd DistributedFileDownloader
mvn clean compile
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency
```

### **3.3. Cấu hình Slave Server**
```properties
# Slave Server Configuration (macOS)
servers=192.168.1.2:12345,192.168.1.3:12346,192.168.1.4:12347
master_server=192.168.1.2:12345
expected_hash=0708271a3ba129cdd31c697d1023bff3b6474cdd08742c533d9541099d07bbb3
file_path=test.txt
db_path=files.db
```

### **3.4. Khởi động Slave Server**
```bash
# Chạy Slave Server
java -cp "target/classes:target/dependency/*" com.example.Main server 12346
```

## 🪟 **BƯỚC 4: TRIỂN KHAI TRÊN WINDOWS (Slave Server 2)**

### **4.1. Cài đặt môi trường**
```powershell
# Cài đặt Java 11
# Download từ: https://adoptium.net/
# Cài đặt Maven
# Download từ: https://maven.apache.org/download.cgi

# Kiểm tra
java -version
mvn -version
```

### **4.2. Clone và build**
```cmd
git clone https://github.com/yourusername/DistributedFileDownloader.git
cd DistributedFileDownloader
mvn clean compile
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency
```

### **4.3. Cấu hình Slave Server**
```properties
# Slave Server Configuration (Windows)
servers=192.168.1.2:12345,192.168.1.3:12346,192.168.1.4:12347
master_server=192.168.1.2:12345
expected_hash=0708271a3ba129cdd31c697d1023bff3b6474cdd08742c533d9541099d07bbb3
file_path=test.txt
db_path=files.db
```

### **4.4. Khởi động Slave Server**
```cmd
java -cp "target/classes;target/dependency/*" com.example.Main server 12347
```

## 🔧 **BƯỚC 5: CẤU HÌNH NETWORK**

### **5.1. Firewall Configuration**

#### **Linux (Master Server)**
```bash
# Ubuntu/Debian
sudo ufw allow 12345
sudo ufw allow 12346
sudo ufw allow 12347
sudo ufw enable

# CentOS/RHEL
sudo firewall-cmd --permanent --add-port=12345/tcp
sudo firewall-cmd --permanent --add-port=12346/tcp
sudo firewall-cmd --permanent --add-port=12347/tcp
sudo firewall-cmd --reload
```

#### **macOS (Slave Server)**
```bash
# Kiểm tra firewall
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate

# Cho phép Java qua firewall
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /usr/bin/java
```

#### **Windows (Slave Server)**
```cmd
# Mở ports trong Windows Firewall
netsh advfirewall firewall add rule name="Distributed File Server" dir=in action=allow protocol=TCP localport=12345,12346,12347
```

### **5.2. Network Testing**
```bash
# Test connectivity từ Linux Master
telnet 192.168.1.3 12346  # macOS Slave
telnet 192.168.1.4 12347  # Windows Slave

# Test từ macOS Slave
telnet 192.168.1.2 12345  # Linux Master
telnet 192.168.1.4 12347  # Windows Slave

# Test từ Windows Slave
telnet 192.168.1.2 12345  # Linux Master
telnet 192.168.1.3 12346  # macOS Slave
```

## 🎯 **BƯỚC 6: CHẠY CLIENT**

### **6.1. Cấu hình Client**
```properties
# Client Configuration (bất kỳ máy nào)
servers=192.168.1.2:12345,192.168.1.3:12346,192.168.1.4:12347
master_server=192.168.1.2:12345
expected_hash=0708271a3ba129cdd31c697d1023bff3b6474cdd08742c533d9541099d07bbb3
file_path=test.txt
db_path=files.db
```

### **6.2. Chạy Client GUI**
```bash
# Linux/macOS
java -cp "target/classes:target/dependency/*" com.example.Main

# Windows
java -cp "target/classes;target/dependency/*" com.example.Main
```

## 📊 **BƯỚC 7: MONITORING VÀ TESTING**

### **7.1. Kiểm tra Server Status**
```bash
# Trên mỗi máy server
netstat -an | grep "12345\|12346\|12347"

# Kiểm tra logs
tail -f server.log  # nếu có logging
```

### **7.2. Test Download**
```bash
# Test từ client
java -cp "target/classes:target/dependency/*" com.example.client.ConcurrentDownloadClient test.txt
```

### **7.3. Performance Monitoring**
```bash
# Monitor network usage
iftop  # Linux
nethogs  # Linux
netstat -i  # macOS/Windows
```

## 🚨 **TROUBLESHOOTING**

### **Lỗi Kết Nối**
1. **Kiểm tra firewall**: Đảm bảo ports 12345-12347 được mở
2. **Kiểm tra IP**: Verify IP addresses trong config
3. **Test connectivity**: Sử dụng telnet hoặc ping

### **Lỗi Java**
1. **Java version**: Đảm bảo Java 11+ trên tất cả máy
2. **Classpath**: Kiểm tra đường dẫn dependencies
3. **Permissions**: Đảm bảo quyền đọc/ghi file

### **Lỗi Database**
1. **File permissions**: Kiểm tra quyền ghi database
2. **Disk space**: Đảm bảo đủ dung lượng
3. **Corruption**: Xóa và tạo lại files.db nếu cần

## 📈 **OPTIMIZATION**

### **Performance Tuning**
1. **JVM parameters**: `-Xmx2g -Xms1g`
2. **Network buffer**: Tăng buffer size cho file lớn
3. **Thread pool**: Điều chỉnh số threads

### **Security**
1. **VPN**: Sử dụng VPN cho mạng nội bộ
2. **Authentication**: Thêm user authentication
3. **Encryption**: Encrypt sensitive data

## 📞 **SUPPORT**

Nếu gặp vấn đề:
1. Kiểm tra logs trên tất cả servers
2. Verify network connectivity
3. Test với file nhỏ trước
4. Check system resources (CPU, RAM, Disk)

---

**🎉 Chúc mừng! Bạn đã triển khai thành công hệ thống Distributed File Downloader trên đa nền tảng!**
