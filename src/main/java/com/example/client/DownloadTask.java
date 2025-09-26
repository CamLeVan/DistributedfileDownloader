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

    public DownloadTask(String host, int port, int chunkIndex, long chunkSize, long fileSize, int totalServers) {
        this.host = host;
        this.port = port;
        this.chunkIndex = chunkIndex;
        this.chunkSize = chunkSize;
        this.fileSize = fileSize;
        this.totalServers = totalServers;
    }

    @Override
    public byte[] call() throws Exception {
        // FIXED: stepIndex đúng: chunk 0 ở row 1 (sau bước 1: khởi tạo), chunk 1 ở row 2, v.v.
        int stepIndex = 1 + chunkIndex;
        DownloadGUI.tableModel.setValueAt("Đang thực hiện", stepIndex, 5);
        DownloadGUI.log("Thread " + Thread.currentThread().getName() + ": Bắt đầu tải chunk " + chunkIndex + " từ " + host + ":" + port);
        long offset = calculateOffset(chunkIndex, fileSize, totalServers);
        long toRead = Math.min(chunkSize, fileSize - offset);
        if (toRead <= 0) {
            DownloadGUI.log("Thread " + Thread.currentThread().getName() + ": Chunk " + chunkIndex + " đã hoàn tất");
            DownloadGUI.tableModel.setValueAt("Hoàn tất", stepIndex, 5);
            return new byte[0];
        }
        // FIXED: Thêm check overflow cho byte[] (nếu file >2GB, chunk lớn)
        if (toRead > Integer.MAX_VALUE) {
            throw new IOException("Chunk too large for memory: " + toRead);
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            try (Socket socket = new Socket()) {
                socket.setSoTimeout(5000);
                socket.connect(new InetSocketAddress(host, port), 5000);
                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedInputStream in = new BufferedInputStream(socket.getInputStream())) {
                    out.println("GET " + chunkIndex + " " + toRead + " " + totalServers); // Thêm totalServers
                    byte[] chunk = new byte[(int) toRead];
                    int offsetInChunk = 0;
                    while (toRead > 0) {
                        int len = in.read(chunk, offsetInChunk, (int) Math.min(8192, toRead));
                        if (len == -1) break;
                        offsetInChunk += len;
                        toRead -= len;
                    }
                    if (offsetInChunk != chunk.length) {
                        throw new IOException("Received chunk size " + offsetInChunk + " does not match expected " + chunk.length);
                    }
                    DownloadGUI.log("Thread " + Thread.currentThread().getName() + ": Hoàn thành chunk " + chunkIndex + " at offset " + offset);
                    DownloadGUI.tableModel.setValueAt("Hoàn tất", stepIndex, 5);
                    return chunk;
                }
            } catch (IOException e) {
                System.err.println("Attempt " + (attempt + 1) + " failed for chunk " + chunkIndex + " from " + host + ":" + port + ": " + e.getMessage());
                DownloadGUI.log("Thread " + Thread.currentThread().getName() + ": Retry " + (attempt + 1) + " cho chunk " + chunkIndex);
                if (attempt == 2) throw new IOException("All retries failed for " + host + ":" + port, e);
                Thread.sleep(1000 * (attempt + 1));
            }
        }
        throw new RuntimeException("Unexpected error in DownloadTask");
    }

    private long calculateOffset(int chunkIndex, long fileSize, int totalServers) {
        long baseSize = fileSize / totalServers;
        long remainder = fileSize % totalServers;
        long offset = 0;
        for (int i = 0; i < chunkIndex; i++) {
            offset += baseSize + (i < remainder ? 1 : 0);
        }
        return offset;
    }
}