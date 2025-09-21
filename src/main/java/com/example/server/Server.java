package com.example.server;

import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) { // [TCP Sockets, Step 1/4: Tạo ServerSocket và lắng nghe kết nối]
            System.out.println("Basic Server running on port 12345..."); // [TCP Sockets, Step 2/4: Báo trạng thái server]
            while (true) {
                try (Socket clientSocket = serverSocket.accept(); // [TCP Sockets, Step 3/4: Chấp nhận kết nối từ client]
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); // [TCP Sockets, Step 1/4: Chuẩn bị gửi dữ liệu]
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) { // [TCP Sockets, Step 1/4: Chuẩn bị nhận dữ liệu]
                    String input = in.readLine(); // [TCP Sockets, Step 2/4: Nhận dữ liệu từ client]
                    System.out.println("Received: " + input); // [TCP Sockets, Step 3/4: Xử lý dữ liệu nhận được]
                    out.println("Hello from Basic Server"); // [TCP Sockets, Step 4/4: Gửi phản hồi qua TCP]
                } catch (IOException e) { // [Error Handling, Step 1/2: Bắt ngoại lệ]
                    System.err.println("Error handling client: " + e.getMessage()); // [Error Handling, Step 2/2: Báo cáo lỗi]
                }
            }
        } catch (IOException e) { // [Error Handling, Step 1/2: Bắt ngoại lệ]
            System.err.println("Server error: " + e.getMessage()); // [Error Handling, Step 2/2: Báo cáo lỗi]
        }
    }
}