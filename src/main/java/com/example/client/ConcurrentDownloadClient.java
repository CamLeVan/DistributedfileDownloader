package com.example.client;

import com.example.DownloadGUI;
import com.example.utils.FileUtils;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ConcurrentDownloadClient {
    private static final String[] SERVERS = {"localhost:12345", "localhost:12346"};
    private static final String OUTPUT_FILE = "downloaded_file.txt";
    private static final String EXPECTED_HASH = "3fad459e0dbaaea15a0845d18fbcc27fdb1ae83e64b6f2b4f78c12eae43f7a00";

    public static void main(String[] args) {
        long fileSize = getFileSizeFromServer();
        if (fileSize <= 0) {
            System.err.println("Failed to get file size or size is 0");
            return;
        }
        System.out.println("Bắt đầu tải file với kích thước " + fileSize + " bytes từ " + SERVERS.length + " server");

        long chunkSize = fileSize / SERVERS.length;
        ExecutorService executor = Executors.newFixedThreadPool(SERVERS.length);
        List<Future<byte[]>> futures = new ArrayList<>();

        for (int i = 0; i < SERVERS.length; i++) {
            String[] parts = SERVERS[i].split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            futures.add(executor.submit(new DownloadTask(host, port, i, chunkSize, fileSize)));
            System.out.println("Gửi yêu cầu tải chunk " + i + " đến server " + SERVERS[i]);
        }

        try {
            List<byte[]> chunks = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                byte[] chunk = futures.get(i).get();
                chunks.add(chunk);
                System.out.println("Thread " + Thread.currentThread().getName() + ": Hoàn thành chunk " + i);
            }
            executor.shutdown();

            try (RandomAccessFile file = new RandomAccessFile(OUTPUT_FILE, "rw")) {
                long position = 0;
                for (byte[] chunk : chunks) {
                    file.seek(position);
                    file.write(chunk);
                    position += chunk.length;
                }
            }

            String computedHash = FileUtils.calculateHash(OUTPUT_FILE, "SHA-256");
            if (computedHash.equals(EXPECTED_HASH)) {
                System.out.println("Download successful! File integrity verified.");
                System.out.println("Ghép file hoàn tất, xác thực hash...");
            } else {
                System.out.println("Error: File integrity check failed. Hash mismatch. Expected: " + EXPECTED_HASH + ", Got: " + computedHash);
            }
            System.out.println("Download completed");
        } catch (Exception e) {
            System.err.println("Download error: " + e.getMessage());
        }
    }

    private static long getFileSizeFromServer() {
        for (String server : SERVERS) {
            String[] parts = server.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            try (Socket socket = new Socket(host, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out.println("GET_SIZE");
                String sizeStr = in.readLine();
                if (sizeStr != null && !sizeStr.isEmpty() && !sizeStr.startsWith("ERROR")) {
                    return Long.parseLong(sizeStr.trim());
                } else if (sizeStr != null && sizeStr.startsWith("ERROR")) {
                    System.err.println("Server error response from " + server + ": " + sizeStr);
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("Failed to get file size from " + server + ": " + e.getMessage());
            }
        }
        System.err.println("Failed to get file size from all servers");
        return -1;
    }
}