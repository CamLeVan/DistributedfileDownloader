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

        List<String> activeServers = getActiveServers(servers);
        if (activeServers.isEmpty()) {
            System.err.println("No active servers found");
            return;
        }

        long fileSize = getFileSizeFromServer(activeServers.toArray(new String[0]));
        if (fileSize == -1) {
            System.err.println("Failed to get file size from active servers");
            return;
        }

        // FIXED: Init file full size trước khi tải để tránh issue khi write chunks
        FileUtils.initFile(OUTPUT_FILE, fileSize);

        DownloadGUI.log("Bắt đầu tải file với kích thước " + fileSize + " bytes từ " + activeServers.size() + " server");

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
                futures.add(executor.submit(new DownloadTask(host, port, i, chunkSizes[i], fileSize, activeServers.size())));
            }

            try {
                boolean success = true;
                List<String> failedServers = new ArrayList<>();
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        byte[] chunk = futures.get(i).get();
                        if (chunk.length > 0) {
                            FileUtils.saveChunk(chunk, OUTPUT_FILE, calculateOffset(chunkIndices.get(i), chunkSizes));
                        }
                    } catch (Exception e) {
                        System.err.println("Chunk " + chunkIndices.get(i) + " failed from " + activeServers.get(i) + ": " + e.getMessage());
                        failedServers.add(activeServers.get(i));
                        success = false;
                    }
                }
                executor.shutdown();
                waitForCompletion(executor);

                if (success) {
                    break;
                } else {
                    // FIXED: Xóa partial file để retry full với remaining servers (tránh offset sai khi numServers thay đổi)
                    new File(OUTPUT_FILE).delete();
                    for (String failed : failedServers) {
                        activeServers.remove(failed);
                    }
                    if (activeServers.isEmpty()) {
                        System.err.println("No more servers available");
                        return;
                    }
                    System.err.println("Retry with remaining servers: " + activeServers.size());
                }
            } catch (Exception e) {
                System.err.println("Download attempt failed: " + e.getMessage());
                executor.shutdownNow();
                if (activeServers.size() <= 1) {
                    System.err.println("No more servers available after shutdown");
                    break;
                }
            }
        }

        String computedHash = FileUtils.calculateHash(OUTPUT_FILE, "SHA-256");
        if (computedHash.equals(expectedHash)) {
            System.out.println("Download successful! File integrity verified.");
            DownloadGUI.log("Ghép file hoàn tất, xác thực hash...");
        } else {
            System.out.println("Error: File integrity check failed. Hash mismatch: expected " + expectedHash + ", got " + computedHash);
        }
    }

    private static List<String> getActiveServers(String[] servers) {
        List<String> activeServers = new ArrayList<>();
        for (String server : servers) {
            String[] parts = server.split(":");
            if (parts.length != 2) continue;
            String host = parts[0].trim();
            int port;
            try {
                port = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid port for server " + server + ": " + e.getMessage());
                continue;
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000);
                activeServers.add(server);
            } catch (IOException e) {
                System.err.println("Server " + server + " not active: " + e.getMessage());
            }
        }
        return activeServers;
    }

    private static long getFileSizeFromServer(String[] servers) {
        for (String server : servers) {
            String[] parts = server.split(":");
            if (parts.length != 2) continue;
            String host = parts[0].trim();
            int port = Integer.parseInt(parts[1].trim());
            // FIXED: Thêm connect timeout để tránh hang nếu server không phản hồi
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000);
                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    out.println("GET_SIZE");
                    String sizeStr = in.readLine();
                    if (sizeStr != null && !sizeStr.isEmpty() && !sizeStr.startsWith("ERROR")) {
                        return Long.parseLong(sizeStr.trim());
                    }
                }
            } catch (IOException | NumberFormatException e) {
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
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        try {
            downloadFile("");
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}