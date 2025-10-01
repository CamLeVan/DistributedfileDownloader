package com.example.client;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        String host = "localhost";
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 12345; // Mặc định 12345
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000); // Tout.println("Connected to " + host + ":" + port); // Thêm log thành công
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out.println("GET_SIZE");
                String response = in.readLine();
                if (response != null && response.startsWith("ERROR")) {
                    System.err.println("Server error: " + response);
                    return;
                }
                System.out.println("Server response: " + response);
            }
        } catch (IOException e) {
            System.err.println("Failed to connect to " + host + ":" + port + ": " + e.getMessage()); // Thêm chi tiết host:port
        }
    }
}