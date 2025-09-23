package com.example.server;

import com.example.utils.ConfigLoader;

import java.io.*;
import java.net.*;

public class MultiThreadedServer {
    private static final String FILE_PATH = System.getProperty("user.dir") + "/src/main/resources/" + ConfigLoader.getFilePath();

    public static void main(String[] args) {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 12347;
        File file = new File(FILE_PATH);
        if (!file.exists() || file.length() == 0) {
            System.err.println("Error: File " + FILE_PATH + " not found or empty at " + file.getAbsolutePath());
            return;
        }
        System.out.println("File found: " + file.getAbsolutePath() + ", size: " + file.length() + " bytes");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("MultiThreadedServer running on port " + port + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, FILE_PATH)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}