package com.example.client;

import com.example.DownloadGUI;

import java.io.*;
import java.net.*;
import java.util.concurrent.Callable;

public class DownloadTask implements Callable<byte[]> {
    private final String host;
    private final int port;
    private final int chunkIndex;
    private final long chunkSize;
    private final long fileSize;
    private final int totalServers;
    private final String fileName;  // NEW: File name

    public DownloadTask(String host, int port, int chunkIndex, long chunkSize, long fileSize, int totalServers, String fileName) {
        this.host = host;
        this.port = port;
        this.chunkIndex = chunkIndex;
        this.chunkSize = chunkSize;
        this.fileSize = fileSize;
        this.totalServers = totalServers;
        this.fileName = fileName;
    }

    @Override
    public byte[] call() throws Exception {
        int stepIndex = 1 + chunkIndex;
        DownloadGUI.tableModel.setValueAt("Đang thực hiện " + fileName, stepIndex, 5);
        DownloadGUI.log("Thread " + Thread.currentThread().getName() + ": Bắt đầu tải chunk " + chunkIndex + " của " + fileName + " từ " + host + ":" + port);
        long offset = calculateOffset(chunkIndex, fileSize, totalServers);
        long toRead = Math.min(chunkSize, fileSize - offset);
        if (toRead <= 0) {
            DownloadGUI.tableModel.setValueAt("Hoàn tất " + fileName, stepIndex, 5);
            return new byte[0];
        }
        if (toRead > Integer.MAX_VALUE) {
            throw new IOException("Chunk too large: " + toRead);
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            try (Socket socket = new Socket()) {
                socket.setSoTimeout(5000);
                socket.connect(new InetSocketAddress(host, port), 5000);
                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedInputStream in = new BufferedInputStream(socket.getInputStream())) {
                    // NEW: Include fileName in GET
                    out.println("GET " + fileName + " " + chunkIndex + " " + toRead + " " + totalServers);
                    byte[] chunk = new byte[(int) toRead];
                    int offsetInChunk = 0;
                    long remaining = toRead;
                    while (remaining > 0) {
                        int len = in.read(chunk, offsetInChunk, (int) Math.min(8192, remaining));
                        if (len == -1) break;
                        offsetInChunk += len;
                        remaining -= len;
                    }
                    if (offsetInChunk != chunk.length) {
                        throw new IOException("Received " + offsetInChunk + " != expected " + chunk.length);
                    }
                    DownloadGUI.log("Thread " + Thread.currentThread().getName() + ": Hoàn thành chunk " + chunkIndex + " của " + fileName);
                    DownloadGUI.tableModel.setValueAt("Hoàn tất " + fileName, stepIndex, 5);
                    return chunk;
                }
            } catch (IOException e) {
                System.err.println("Attempt " + (attempt + 1) + " failed for chunk " + chunkIndex + " of " + fileName + ": " + e.getMessage());
                DownloadGUI.log("Retry " + (attempt + 1) + " cho chunk " + chunkIndex + " của " + fileName);
                if (attempt == 2) throw new IOException("All retries failed", e);
                Thread.sleep(1000 * (attempt + 1));
            }
        }
        throw new RuntimeException("Unexpected error");
    }

    private long calculateOffset(int chunkIndex, long fileSize, int totalServers) {
        // Unchanged
        long baseSize = fileSize / totalServers;
        long remainder = fileSize % totalServers;
        long offset = 0;
        for (int i = 0; i < chunkIndex; i++) {
            offset += baseSize + (i < remainder ? 1 : 0);
        }
        return offset;
    }
}