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
                bos.write("ERROR: No command received\n".getBytes());
                bos.flush();
                return;
            }

            File file = new File(filePath);
            if (!file.exists() || file.length() == 0) {
                bos.write("ERROR: File not found or empty\n".getBytes());
                bos.flush();
                return;
            }

            if ("GET_SIZE".equals(command)) {
                bos.write(Long.toString(file.length()).getBytes());
                bos.write("\n".getBytes());
                bos.flush();
                return;
            }

            if (command.startsWith("GET ")) {
                String[] parts = command.split(" ");
                if (parts.length != 4) { // Thêm trường totalServers
                    bos.write("ERROR: Invalid GET format\n".getBytes());
                    bos.flush();
                    return;
                }
                int chunkIndex = Integer.parseInt(parts[1]);
                long chunkSize = Long.parseLong(parts[2]);
                int totalServers = Integer.parseInt(parts[3]); // Nhận số server từ client
                long offset = calculateOffset(chunkIndex, file.length(), totalServers);
                if (offset >= file.length()) {
                    bos.write("ERROR: Invalid chunk index\n".getBytes());
                    bos.flush();
                    return;
                }

                try (FileInputStream fis = new FileInputStream(file)) {
                    if (fis.skip(offset) != offset) {
                        bos.write("ERROR: Failed to skip to offset\n".getBytes());
                        bos.flush();
                        return;
                    }
                    long remaining = Math.min(chunkSize, file.length() - offset);
                    byte[] buffer = new byte[8192];
                    while (remaining > 0) {
                        int len = fis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (len == -1) break;
                        bos.write(buffer, 0, len);
                        remaining -= len;
                    }
                    bos.flush();
                    System.out.println("Sent chunk " + chunkIndex + " from offset " + offset + " with size " + chunkSize);
                }
            } else {
                bos.write("ERROR: Invalid command\n".getBytes());
                bos.flush();
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error in handler: " + e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
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

    private long[] calculateChunkSizes(long fileSize, int serverCount) {
        long baseSize = fileSize / serverCount;
        long remainder = fileSize % serverCount;
        long[] chunkSizes = new long[serverCount];
        for (int i = 0; i < serverCount; i++) {
            chunkSizes[i] = baseSize + (i < remainder ? 1 : 0);
        }
        return chunkSizes;
    }
}