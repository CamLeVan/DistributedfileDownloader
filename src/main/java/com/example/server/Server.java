package com.example.server;

import com.example.utils.ConfigLoader;
import java.io.*;
import java.net.*;

public class Server {
    private static final String FILE_PATH = System.getProperty("user.dir") + "/src/main/resources/" + ConfigLoader.getFilePath();

    public static void main(String[] args) {
        int port = 12345;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server running on port " + port + "...");
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket, FILE_PATH)).start();
                } catch (IOException e) {
                    System.err.println("Accept error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}