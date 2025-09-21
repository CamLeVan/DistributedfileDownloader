package com.example.server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final String filePath;

    public ClientHandler(Socket socket, String filePath) {
        this.socket = socket;
        this.filePath = filePath;
    }

    @Override
    public void run() {
        try (BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
             BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(bis))) {

            String command = reader.readLine();
            if (command == null) {
                bos.write("ERROR: No command\n".getBytes());
                bos.flush();
                return;
            }

            if (command.equals("GET_SIZE")) {
                File file = new File(filePath);
                if (file.exists() && file.length() > 0) {
                    long fileSize = file.length();
                    bos.write(Long.toString(fileSize).getBytes());
                    bos.write("\n".getBytes());
                    bos.flush();
                } else {
                    bos.write("ERROR: File not found or empty\n".getBytes());
                    bos.flush();
                }
                return;
            }

            if (!command.startsWith("GET ")) {
                bos.write("ERROR: Invalid command\n".getBytes());
                bos.flush();
                return;
            }

            String[] parts = command.split(" ");
            int chunkIndex = Integer.parseInt(parts[1]);
            long chunkSize = Long.parseLong(parts[2]);
            long fileSize = new File(filePath).length();

            if (chunkIndex * chunkSize >= fileSize) {
                bos.write("ERROR: Invalid chunk index\n".getBytes());
                bos.flush();
                return;
            }

            try (FileInputStream fis = new FileInputStream(filePath)) {
                long start = chunkIndex * chunkSize;
                long end = Math.min(start + chunkSize, fileSize);
                fis.skip(start);
                byte[] buffer = new byte[8192];
                long remaining = end - start;
                while (remaining > 0) {
                    int len = fis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (len == -1) break;
                    bos.write(buffer, 0, len);
                    remaining -= len;
                }
                bos.flush();
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error in handler: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}