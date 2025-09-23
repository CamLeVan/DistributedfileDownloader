package com.example.client;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.println("GET_SIZE"); // Gửi lệnh hợp lệ
            String response = in.readLine();
            System.out.println("Server response: " + response); // Nên nhận kích thước file
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}