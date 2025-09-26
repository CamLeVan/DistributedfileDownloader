package com.example;

import com.example.client.ConcurrentDownloadClient;
import com.example.server.MultiThreadedServer;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        if (args.length > 0 && "server".equals(args[0])) {
            // FIXED: Pass chỉ args từ index 1 trở đi cho server (tránh parse "server" as port)
            String[] serverArgs = (args.length > 1) ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[0];
            MultiThreadedServer.main(serverArgs);
        } else {
            new DownloadGUI().setVisible(true); // Chạy GUI thay vì client trực tiếp
        }
    }
}