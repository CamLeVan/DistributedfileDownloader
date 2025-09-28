package com.example.client;

import com.example.DownloadGUI;
import com.example.utils.DBUtils;
import com.example.utils.FileUtils;
import com.example.utils.ConfigLoader;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

public class ConcurrentDownloadClient {
    private static final String OUTPUT_FILE = "downloaded_file.txt";  // Dynamic name sau

    public static List<String[]> getFileListFromMaster() {
        List<String[]> files = new ArrayList<>();
        String master = ConfigLoader.getMasterServer();
        String[] parts = master.split(":");
        if (parts.length != 2) {
            System.err.println("Invalid master server: " + master);
            return files;
        }
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out.println("LIST");
                String line;
                while ((line = in.readLine()) != null && !line.equals("END")) {
                    if (line.isEmpty()) continue;
                    String[] arr = line.split("\\|");
                    if (arr.length == 3) files.add(arr);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Failed to get file list from master " + master + ": " + e.getMessage());
        }
        return files;
    }

    // FIXED: Query GET_INFO cho size + hash thật
    public static String[] getFileInfoFromMaster(String fileName) {
        String master = ConfigLoader.getMasterServer();
        String[] parts = master.split(":");
        if (parts.length != 2) return null;
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out.println("GET_INFO " + fileName);
                String sizeStr = in.readLine();
                if (sizeStr != null && !sizeStr.startsWith("ERROR")) {
                    String hashStr = in.readLine();  // Đọc hash line tiếp theo
                    if (hashStr != null && !hashStr.startsWith("ERROR")) {
                        return new String[]{sizeStr.trim(), hashStr.trim()};
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Failed to get file info for " + fileName + ": " + e.getMessage());
        }
        return null;
    }


    public static void downloadFile(String fileName) throws IOException, NoSuchAlgorithmException {
        if (fileName.isEmpty()) {
            System.err.println("No file name specified");
            return;
        }
        String outputPath = fileName;
        String[] info = getFileInfoFromMaster(fileName);
        if (info == null || info[0].isEmpty() || info[1].isEmpty()) {
            System.err.println("Failed to get info for " + fileName);
            return;
        }
        long fileSize = Long.parseLong(info[0]);
        String expectedHash = info[1];

        // NEW: Resume logic
        File outputFile = new File(outputPath);
        Map<Integer, Long> progress = DBUtils.getProgress(fileName);
        if (outputFile.exists()) {
            long existingSize = outputFile.length();
            if (existingSize == fileSize) {
                String existingHash = FileUtils.calculateHash(outputPath, "SHA-256");
                if (existingHash.equals(expectedHash)) {
                    System.out.println("Resume: File " + fileName + " already complete and verified.");
                    DownloadGUI.log("Resume: " + fileName + " đã hoàn chỉnh, skip.");
                    DBUtils.clearProgress(fileName);  // Clear sau success
                    return;
                }
            }
            // Partial: Adjust - assume sequential chunks, skip done chunks
            DownloadGUI.log("Resume partial " + fileName + " from " + existingSize + " bytes (progress: " + progress.size() + " chunks)");
        } else {
            progress.clear();  // New download
        }

        String[] servers = ConfigLoader.getServers();
        if (servers.length == 0) {
            System.err.println("No servers configured");
            return;
        }

        List<String> activeServers = getActiveServers(servers);
        if (activeServers.isEmpty()) {
            System.err.println("No active servers found");
            return;
        }

        FileUtils.initFile(outputPath, fileSize);  // Always init full

        DownloadGUI.log("Bắt đầu tải/resume " + fileName + " (" + fileSize + " bytes) từ " + activeServers.size() + " servers");

        while (!activeServers.isEmpty()) {
            long[] chunkSizes = calculateChunkSizes(fileSize, activeServers.size());
            // NEW: Adjust chunks based on progress (skip done, reduce size for partial)
            for (int i = 0; i < chunkSizes.length; i++) {
                if (progress.containsKey(i)) {
                    long done = progress.get(i);
                    if (done >= chunkSizes[i]) {
                        chunkSizes[i] = 0;  // Skip full chunk
                        DownloadGUI.log("Skipping completed chunk " + i + " for " + fileName);
                    } else {
                        chunkSizes[i] -= done;  // Resume from done bytes
                    }
                }
            }

            ExecutorService executor = Executors.newFixedThreadPool(activeServers.size());
            List<Future<byte[]>> futures = new ArrayList<>();
            List<Integer> chunkIndices = new ArrayList<>();

            for (int i = 0; i < activeServers.size(); i++) {
                if (chunkSizes[i] <= 0) continue;  // Skip done chunks
                String[] parts = activeServers.get(i).split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                chunkIndices.add(i);
                futures.add(executor.submit(new DownloadTask(host, port, i, chunkSizes[i], fileSize, activeServers.size(), fileName)));
            }

            if (futures.isEmpty()) {  // All chunks done
                System.out.println("All chunks complete for " + fileName);
                break;
            }

            try {
                boolean success = true;
                List<String> failedServers = new ArrayList<>();
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        byte[] chunk = futures.get(i).get();
                        int fullIndex = chunkIndices.get(i);
                        long offset = calculateOffset(fullIndex, chunkSizes);  // Adjust offset for partial
                        if (chunk.length > 0) {
                            FileUtils.saveChunk(chunk, outputPath, offset);
                            DBUtils.saveProgress(fileName, fullIndex, chunkSizes[fullIndex]);  // Save after success
                        }
                    } catch (Exception e) {
                        System.err.println("Chunk " + chunkIndices.get(i) + " failed for " + fileName + " from " + activeServers.get(i) + ": " + e.getMessage());
                        failedServers.add(activeServers.get(i));
                        success = false;
                    }
                }
                executor.shutdown();
                waitForCompletion(executor);

                if (success) {
                    DBUtils.clearProgress(fileName);  // Clear on full success
                    break;
                } else {
                    new File(outputPath).delete();  // Reset partial on fail
                    for (String failed : failedServers) {
                        activeServers.remove(failed);
                    }
                    if (activeServers.isEmpty()) {
                        System.err.println("No more servers available");
                        return;
                    }
                    System.err.println("Retry " + fileName + " with remaining servers: " + activeServers.size());
                }
            } catch (Exception e) {
                System.err.println("Download attempt failed for " + fileName + ": " + e.getMessage());
                executor.shutdownNow();
                if (activeServers.size() <= 1) {
                    System.err.println("No more servers available after shutdown");
                    break;
                }
            }
        }

        String computedHash = FileUtils.calculateHash(outputPath, "SHA-256");
        if (computedHash.equals(expectedHash)) {
            System.out.println("Download " + fileName + " successful! Hash verified.");
            DownloadGUI.log("Ghép " + fileName + " hoàn tất, hash OK");
        } else {
            System.out.println("Error: Hash mismatch for " + fileName + ". Expected: " + expectedHash + ", Got: " + computedHash);
        }
    }



    // FIXED: Full body từ code gốc
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

    private static long getFileSizeFromServer(String[] servers, String fileName) {
        for (String server : servers) {
            String[] parts = server.split(":");
            if (parts.length != 2) continue;
            String host = parts[0].trim();
            int port = Integer.parseInt(parts[1].trim());
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000);
                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    out.println("GET_SIZE " + fileName);
                    String sizeStr = in.readLine();
                    if (sizeStr != null && !sizeStr.isEmpty() && !sizeStr.startsWith("ERROR")) {
                        return Long.parseLong(sizeStr.trim());
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to get size for " + fileName + " from " + server + ": " + e.getMessage());
            }
        }
        return -1;
    }

    // FIXED: Full body
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
            downloadFile("test.txt");
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}