package com.example.client;

import com.example.DownloadGUI;
import com.example.utils.FileUtils;
import com.example.utils.ConfigLoader;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

public class ConcurrentDownloadClient {
    private static final String OUTPUT_FILE = "downloaded_file.txt";

    public static void downloadFile(String url) throws IOException, NoSuchAlgorithmException {
        String[] servers = ConfigLoader.getServers();
        if (servers.length == 0) {
            System.err.println("No servers configured");
            return;
        }
        String expectedHash = ConfigLoader.getExpectedHash();

        long fileSize = getFileSizeFromServer(servers);
        if (fileSize == -1) {
            System.err.println("Failed to get file size from all servers");
            return;
        }

        DownloadGUI.log("Bắt đầu tải file với kích thước " + fileSize + " bytes từ " + servers.length + " server");

        List<String> activeServers = new ArrayList<>(Arrays.asList(servers));
        while (!activeServers.isEmpty()) {
            long[] chunkSizes = calculateChunkSizes(fileSize, activeServers.size());
            ExecutorService executor = Executors.newFixedThreadPool(activeServers.size());
            List<Future<byte[]>> futures = new ArrayList<>();
            List<Integer> chunkIndices = new ArrayList<>();

            for (int i = 0; i < activeServers.size(); i++) {
                String[] parts = activeServers.get(i).split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                chunkIndices.add(i);
                futures.add(executor.submit(new DownloadTask(host, port, i, chunkSizes[i], fileSize)));
            }

            try {
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        byte[] chunk = futures.get(i).get();
                        FileUtils.saveChunk(chunk, OUTPUT_FILE, calculateOffset(chunkIndices.get(i), chunkSizes));
                    } catch (Exception e) {
                        System.err.println("Chunk " + chunkIndices.get(i) + " failed: " + e.getMessage());
                        activeServers.remove(i);
                        i--; // Quay lại để xử lý lại index sau khi remove
                        continue;
                    }
                }
                executor.shutdown();
                waitForCompletion(executor);
                break; // Thoát nếu tất cả chunk tải thành công
            } catch (Exception e) {
                System.err.println("Download attempt failed, retrying with remaining servers: " + e.getMessage());
                executor.shutdownNow();
                if (activeServers.size() == 1) {
                    System.err.println("No more servers available");
                    return;
                }
            }
        }

        String computedHash = FileUtils.calculateHash(OUTPUT_FILE, "SHA-256");
        if (computedHash.equals(expectedHash)) {
            System.out.println("Download successful! File integrity verified.");
            DownloadGUI.log("Ghép file hoàn tất, xác thực hash...");
        } else {
            System.out.println("Error: File integrity check failed. Hash mismatch.");
        }
    }

    private static long getFileSizeFromServer(String[] servers) {
        for (String server : servers) {
            String[] parts = server.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            try (Socket socket = new Socket(host, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out.println("GET_SIZE");
                String sizeStr = in.readLine();
                if (sizeStr != null && !sizeStr.isEmpty()) {
                    return Long.parseLong(sizeStr.trim());
                }
            } catch (IOException e) {
                System.err.println("Failed to get file size from " + server + ": " + e.getMessage());
            }
        }
        return -1;
    }

    private static long[] calculateChunkSizes(long fileSize, int serverCount) {
        long baseSize = fileSize / serverCount;
        long remainder = fileSize % serverCount;
        long[] chunkSizes = new long[serverCount];
        for (int i = 0; i < serverCount; i++) {
            chunkSizes[i] = baseSize + (i < remainder ? 1 : 0);
        }
        return chunkSizes;
    }

    private static long calculateOffset(int index, long[] chunkSizes) {
        long offset = 0;
        for (int i = 0; i < index; i++) {
            offset += chunkSizes[i];
        }
        return offset;
    }

    private static void waitForCompletion(ExecutorService executor) {
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        downloadFile("");
    }
}