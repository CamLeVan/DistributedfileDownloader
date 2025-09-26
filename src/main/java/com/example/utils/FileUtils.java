package com.example.utils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtils {
    public static void saveChunk(byte[] data, String outputFile, long position) throws IOException {
        // FIXED: Bỏ delete file, chỉ write tại position (file đã set length trước)
        try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
            raf.seek(position);
            raf.write(data);
        } catch (IOException e) {
            throw new IOException("Failed to save chunk at position " + position + ": " + e.getMessage(), e);
        }
    }

    public static String calculateHash(String filePath, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        try (FileInputStream fis = new FileInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // FIXED: Thêm method helper để init file full size (gọi từ client)
    public static void initFile(String outputFile, long fileSize) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
            raf.setLength(fileSize);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java FileUtils <file_path>");
            System.exit(1);
        }
        String filePath = args[0];
        String hash = calculateHash(filePath, "SHA-256");
        System.out.println("Hash of " + filePath + ": " + hash);
    }
}