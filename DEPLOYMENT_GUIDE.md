# 🚀 Hướng Dẫn Triển Khai Hệ Thống Tải File Phân Tán

## 📋 Tổng Quan

Hệ thống **Client-Multiple Servers** cho phép tải file từ nhiều server đồng thời, tăng tốc độ download và đảm bảo tính toàn vẹn dữ liệu.

## 🏗️ Kiến Trúc Hệ Thống

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Client    │    │   Master    │    │   Slave 1   │
│   (GUI)     │◄──►│   Server    │◄──►│   Server    │
│             │    │  (Port 12345)│    │  (Port 12346)│
└─────────────┘    └─────────────┘    └─────────────┘
                           │
                           ▼
                   ┌─────────────┐
                   │   Slave 2   │
                   │   Server    │
                   │  (Port 12347)│
                   └─────────────┘
```

## 🎯 Vai Trò Của Từng Server

### **Master Server (Port 12345)**
- 📋 **Quản lý metadata**: Lưu trữ thông tin file (tên, kích thước, hash)
- 🔍 **Cung cấp danh sách**: Trả về danh sách file có sẵn
- 📊 **Phân phối chunks**: Chia file thành các phần và phân phối
- 🗄️ **Database**: Quản lý SQLite database

### **Slave Servers (Port 12346, 12347)**
- 📦 **Lưu trữ file**: Chứa các file thực tế
- ⚡ **Phục vụ chunks**: Gửi các phần file theo yêu cầu
- 🔄 **Xử lý đồng thời**: Hỗ trợ multiple clients

## 🖥️ Triển Khai Trên Các Hệ Điều Hành

### **Windows**
```bash
# 1. Build project
mvn clean compile
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency

# 2. Chạy tất cả servers
start_servers.bat
```

### **Linux/macOS**
```bash
# 1. Build project
mvn clean compile
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency

# 2. Cấp quyền thực thi
chmod +x start_servers.sh

# 3. Chạy tất cả servers
./start_servers.sh
```

### **macOS (Double-click)**
```bash
# Double-click file start_servers.command
```

## 🌐 Triển Khai Trên Nhiều Máy

### **Cấu Hình Network**

1. **Máy 1 (Master)**: `192.168.1.100:12345`
2. **Máy 2 (Slave 1)**: `192.168.1.101:12346`  
3. **Máy 3 (Slave 2)**: `192.168.1.102:12347`
4. **Máy 4 (Client)**: `192.168.1.103`

### **Cập Nhật Config**

Sửa file `application.properties`:
```properties
servers=192.168.1.100:12345,192.168.1.101:12346,192.168.1.102:12347
master_server=192.168.1.100:12345
```

### **Chạy Trên Từng Máy**

**Máy Master:**
```bash
java -cp "target/classes:target/dependency/*" com.example.Main server 12345
```

**Máy Slave 1:**
```bash
java -cp "target/classes:target/dependency/*" com.example.Main server 12346
```

**Máy Slave 2:**
```bash
java -cp "target/classes:target/dependency/*" com.example.Main server 12347
```

**Máy Client:**
```bash
java -cp "target/classes:target/dependency/*" com.example.Main
```

## 🔧 Cấu Hình Firewall

### **Windows**
```cmd
# Mở port 12345, 12346, 12347
netsh advfirewall firewall add rule name="Distributed File Server" dir=in action=allow protocol=TCP localport=12345,12346,12347
```

### **Linux/macOS**
```bash
# Mở port 12345, 12346, 12347
sudo ufw allow 12345
sudo ufw allow 12346  
sudo ufw allow 12347
```

## 📊 Monitoring & Logs

### **Kiểm Tra Trạng Thái Servers**
```bash
# Windows
netstat -an | findstr "12345\|12346\|12347"

# Linux/macOS
netstat -an | grep "12345\|12346\|12347"
```

### **Log Files**
- Server logs: Console output
- Database: `files.db` (SQLite)
- Downloaded files: Current directory

## 🚨 Troubleshooting

### **Lỗi Kết Nối**
1. Kiểm tra firewall settings
2. Verify IP addresses và ports
3. Check network connectivity

### **Lỗi Database**
1. Kiểm tra quyền ghi file
2. Verify SQLite JDBC driver
3. Check disk space

### **Lỗi Download**
1. Verify file tồn tại trên servers
2. Check hash verification
3. Monitor server logs

## 📈 Performance Tuning

### **Tối Ưu Hóa**
- Tăng buffer size cho file lớn
- Điều chỉnh thread pool size
- Sử dụng SSD cho database

### **Load Balancing**
- Phân phối file đều trên các servers
- Monitor server load
- Auto-failover khi server down

## 🔒 Security

### **Bảo Mật Cơ Bản**
- Sử dụng VPN cho mạng nội bộ
- Encrypt sensitive data
- Regular backup database

### **Authentication (Tương Lai)**
- User authentication
- Access control
- Audit logging

## 📞 Support

Nếu gặp vấn đề, kiểm tra:
1. Log files
2. Network connectivity  
3. Server status
4. Database integrity
