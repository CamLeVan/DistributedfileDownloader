package com.example.server;

import com.example.utils.ConfigLoader;
import com.example.utils.DBUtils;

import java.io.*;
import java.net.*;

public class MultiThreadedServer {
    private static final String BASE_FILE_PATH = System.getProperty("user.dir") + "/src/main/resources/";
    private static final boolean IS_MASTER = ConfigLoader.getServers()[0].equals(ConfigLoader.getMasterServer());

    public static void main(String[] args) {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 12347;
        new File(BASE_FILE_PATH).mkdirs();

        if (IS_MASTER) {
            System.out.println("Master server: Init DB at " + ConfigLoader.getDbPath());
            DBUtils.initDB(); // Chỉ gọi một lần từ main
        }

        File sampleFile = new File(BASE_FILE_PATH + ConfigLoader.getFilePath());
        if (!sampleFile.exists() || sampleFile.length() == 0) {
            System.err.println("Error: Sample file " + sampleFile.getAbsolutePath() + " not found or empty");
            return;
        }
        if (!sampleFile.canRead()) {
            System.err.println("Error: No read permission for " + sampleFile.getAbsolutePath());
            return;
        }
        System.out.println("Files folder: " + BASE_FILE_PATH + ", sample size: " + sampleFile.length() + " bytes");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("MultiThreadedServer running on port " + port + " (Master: " + IS_MASTER + ")...");
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket, BASE_FILE_PATH)).start();
                } catch (IOException e) {
                    System.err.println("Accept error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            DBUtils.close();
        }
    }
}