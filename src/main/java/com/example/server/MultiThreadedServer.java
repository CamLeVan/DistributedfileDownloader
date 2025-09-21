package com.example.server;

import java.io.*;
import java.net.*;

public class MultiThreadedServer {
    private static final String FILE_PATH;

    static {
        URL resource = MultiThreadedServer.class.getClassLoader().getResource("test.txt");
        FILE_PATH = (resource != null) ? resource.getPath() : null;
        if (FILE_PATH == null) {
            System.err.println("Error: test.txt not found in resources. Exiting...");
            System.exit(1);
        }
        System.out.println("File path resolved: " + new File(FILE_PATH).getAbsolutePath());
    }

    public static void main(String[] args) {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 12345;
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