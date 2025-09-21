package com.example;

import com.example.client.ConcurrentDownloadClient;
import com.example.server.MultiThreadedServer;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && "server".equals(args[0])) { // [Protocol Design, Step 1/2: Xử lý lệnh chạy server]
            MultiThreadedServer.main(args); // [Multi-threading, Step 1/2: Chạy server đa luồng]
        } else {
            ConcurrentDownloadClient.main(args); // [Multi-threading, Step 1/2: Chạy client đa luồng]
        }
    }
}