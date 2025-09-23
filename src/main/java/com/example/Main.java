package com.example;

import com.example.client.ConcurrentDownloadClient;
import com.example.server.MultiThreadedServer;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        if (args.length > 0 && "server".equals(args[0])) {
            MultiThreadedServer.main(args);
        } else {
            ConcurrentDownloadClient.main(args);
        }
    }
}