package com.example.client;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345); // [TCP Sockets, Step 1/3: Tạo kết nối client]
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // [TCP Sockets, Step 2/3: Chuẩn bị gửi dữ liệu]
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) { // [TCP Sockets, Step 2/3: Chuẩn bị nhận dữ liệu]
            out.println("Hello from Basic Client"); // [TCP Sockets, Step 3/3: Gửi dữ liệu qua TCP]
            String response = in.readLine(); // [TCP Sockets, Step 3/3: Nhận dữ liệu qua TCP]
            System.out.println("Server response: " + response); // [TCP Sockets, Step 3/3: Xử lý phản hồi]
        } catch (IOException e) { // [Error Handling, Step 1/2: Bắt ngoại lệ mạng]
            System.err.println("Client error: " + e.getMessage()); // [Error Handling, Step 2/2: Báo cáo lỗi]
        }
    }
}