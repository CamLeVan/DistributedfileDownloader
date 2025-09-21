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

    public DownloadTask(String host, int port, int chunkIndex, long chunkSize, long fileSize) {
        this.host = host;
        this.port = port;
        this.chunkIndex = chunkIndex;
        this.chunkSize = chunkSize;
        this.fileSize = fileSize;
    }

    @Override
    public byte[] call() throws Exception {
        DownloadGUI.log("Thread " + Thread.currentThread().getName() + ": Bắt đầu tải chunk " + chunkIndex + " từ server " + host + ":" + port);
        for (int attempt = 0; attempt < 3; attempt++) {
            try (Socket socket = new Socket(host, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedInputStream in = new BufferedInputStream(socket.getInputStream())) {
                out.println("GET " + chunkIndex + " " + chunkSize);
                long end = Math.min((chunkIndex + 1) * chunkSize, fileSize);
                long toRead = end - (chunkIndex * chunkSize);
                byte[] chunk = new byte[(int) toRead];
                int offset = 0;
                while (toRead > 0) {
                    int len = in.read(chunk, offset, (int) Math.min(8192, toRead));
                    if (len == -1) break;
                    offset += len;
                    toRead -= len;
                }
                DownloadGUI.log("Thread " + Thread.currentThread().getName() + ": Hoàn thành chunk " + chunkIndex);
                return chunk;
            } catch (IOException e) {
                System.err.println("Attempt " + (attempt + 1) + " failed for chunk " + chunkIndex + ": " + e.getMessage());
                DownloadGUI.log("Thread " + Thread.currentThread().getName() + ": Retry " + (attempt + 1) + " cho chunk " + chunkIndex);
                if (attempt == 2) throw e;
                Thread.sleep(1000);
            }
        }
        throw new RuntimeException("All retries failed for chunk " + chunkIndex);
    }
}