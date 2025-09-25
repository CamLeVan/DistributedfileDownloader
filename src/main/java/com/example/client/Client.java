package com.example.client;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        String host = "localhost";
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 12347; // Mặc định 12347
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000); // Timeout 5s
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
            System.err.println("Client error: " + e.getMessage());
        }
    }
}