package com.example.server;

import com.example.utils.ConfigLoader;
import com.example.utils.DBUtils;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.stream.Collectors;  // FIXED: Import cho stream

public class ClientHandler implements Runnable {
    private static final boolean IS_MASTER = ConfigLoader.getServers()[0].equals(ConfigLoader.getMasterServer());  // FIXED: Static cho handler
    private final Socket socket;
    private final String baseFilePath;

    public ClientHandler(Socket socket, String baseFilePath) {
        this.socket = socket;
        this.baseFilePath = baseFilePath;
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

            // FIXED: Handle LIST chỉ trên master
            if ("LIST".equals(command)) {
                if (!IS_MASTER) {
                    bos.write("ERROR: Not master server for LIST\n".getBytes());
                    bos.flush();
                    return;
                }
                List<String[]> fileList = DBUtils.getAllFiles();
                String list = String.join("\n", fileList.stream()
                        .map(arr -> String.join("|", arr))
                        .collect(Collectors.toList()));
                bos.write(list.getBytes());
                bos.write("\nEND\n".getBytes());
                bos.flush();
                return;
            }

            // FIXED: GET_INFO cho size + hash
            if ("GET_INFO".equals(command.split(" ", 2)[0])) {
                if (!IS_MASTER) {
                    bos.write("ERROR: Not master for GET_INFO\n".getBytes());
                    bos.flush();
                    return;
                }
                String[] parts = command.split(" ", 2);
                if (parts.length < 2) {
                    bos.write("ERROR: Invalid GET_INFO format\n".getBytes());
                    bos.flush();
                    return;
                }
                String fileName = parts[1].trim();
                String[] info = DBUtils.getFileInfo(fileName);
                if (info != null) {
                    bos.write((info[0] + "\n" + info[1] + "\n").getBytes());
                } else {
                    bos.write(("ERROR: File info not found: " + fileName + "\n").getBytes());
                }
                bos.flush();
                return;
            }

            String[] parts = command.split(" ", 2);
            String cmd = parts[0];

            if ("GET_SIZE".equals(cmd)) {
                String fileName = (parts.length > 1) ? parts[1].trim() : ConfigLoader.getFilePath();
                File file = new File(baseFilePath + fileName);
                if (!file.exists() || file.length() == 0) {
                    bos.write(("ERROR: File not found: " + fileName + "\n").getBytes());
                    bos.flush();
                    return;
                }
                bos.write(Long.toString(file.length()).getBytes());
                bos.write("\n".getBytes());
                bos.flush();
                return;
            }

            if ("GET".equals(cmd)) {
                String[] getParts = parts[1].split(" ");
                if (getParts.length != 4) {
                    bos.write("ERROR: Invalid GET format\n".getBytes());
                    bos.flush();
                    return;
                }
                String getFileName = getParts[0].trim();
                int chunkIndex = Integer.parseInt(getParts[1]);
                long chunkSize = Long.parseLong(getParts[2]);
                int totalServers = Integer.parseInt(getParts[3]);
                File getFile = new File(baseFilePath + getFileName);
                if (!getFile.exists()) {
                    bos.write(("ERROR: File not found: " + getFileName + "\n").getBytes());
                    bos.flush();
                    return;
                }
                long fileSize = getFile.length();
                long offset = calculateOffset(chunkIndex, fileSize, totalServers);
                if (offset >= fileSize) {
                    bos.write("ERROR: Invalid chunk index\n".getBytes());
                    bos.flush();
                    return;
                }

                try (FileInputStream fis = new FileInputStream(getFile)) {
                    if (fis.skip(offset) != offset) {
                        bos.write("ERROR: Failed to skip to offset\n".getBytes());
                        bos.flush();
                        return;
                    }
                    long remaining = Math.min(chunkSize, fileSize - offset);
                    byte[] buffer = new byte[8192];
                    while (remaining > 0) {
                        int len = fis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (len == -1) break;
                        bos.write(buffer, 0, len);
                        remaining -= len;
                    }
                    bos.flush();
                    System.out.println("Sent chunk " + chunkIndex + " of " + getFileName + " from offset " + offset + " with size " + chunkSize);
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
            // FIXED: Không close per handler, để global ở server main
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