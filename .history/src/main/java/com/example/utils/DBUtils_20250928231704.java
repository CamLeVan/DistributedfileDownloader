package com.example.utils;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBUtils {
    private static final String DB_PATH = System.getProperty("user.dir") + "/" + ConfigLoader.getDbPath();
    private static Connection conn;

    static {
        initDB();
    }

    public static void initDB() {
        System.out.println("Starting DB initialization at: " + DB_PATH);
        try {
            File dbFile = new File(DB_PATH);
            if (!dbFile.exists()) {
                System.out.println("DB file not found, attempting to create...");
                if (dbFile.createNewFile()) {
                    System.out.println("Successfully created DB file: " + DB_PATH);
                } else {
                    System.err.println("Failed to create DB file: " + DB_PATH + ", check permissions or disk space");
                    throw new IOException("Cannot create DB file");
                }
            } else {
                System.out.println("DB file exists: " + DB_PATH);
            }

            // Debug classpath
            System.out.println("Classpath: " + System.getProperty("java.class.path"));

            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite JDBC Driver loaded successfully.");

            conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            System.out.println("Connected to DB successfully: " + DB_PATH);

            String createFilesTable = "CREATE TABLE IF NOT EXISTS files (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "size LONG NOT NULL, " +
                    "hash VARCHAR(64) NOT NULL, " +
                    "upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            String createProgressTable = "CREATE TABLE IF NOT EXISTS progress (" +
                    "file_name VARCHAR(255), " +
                    "chunk_index INTEGER, " +
                    "bytes_done LONG, " +
                    "PRIMARY KEY (file_name, chunk_index))";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createFilesTable);
                System.out.println("Created/Checked files table");
                stmt.execute(createProgressTable);
                System.out.println("Created/Checked progress table");
            }

            List<String[]> files = getAllFiles();
            if (files.isEmpty()) {
                String expectedHash = ConfigLoader.getExpectedHash();
                if (expectedHash == null || expectedHash.trim().isEmpty()) {
                    System.err.println("Expected hash is invalid or empty: " + expectedHash);
                    throw new IllegalStateException("Invalid expected hash");
                }
                insertFile("test.txt", 527L, expectedHash);
                insertFile("sample.txt", 360L, "11aa6546294a756a06d1ceb551843d9492d06dd241f8b474dd75697678e78b6c");
                
                // Thêm các file lớn hơn để demo
                insertFile("large_document.txt", 2048L, "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef1234567890");
                insertFile("system_admin.sh", 1536L, "b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef1234567890ab");
                insertFile("DistributedComputingExample.java", 8192L, "c3d4e5f6789012345678901234567890abcdef1234567890abcdef1234567890abcd");
                
                System.out.println("DB init: Added sample files including large files for demo");
            } else {
                System.out.println("DB already contains files, skipping init: " + files.size() + " files");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load SQLite JDBC Driver: " + e.getMessage() + " at line " + e.getStackTrace()[0].getLineNumber());
            e.printStackTrace();
            throw new RuntimeException("Driver load failed", e);
        } catch (SQLException e) {
            System.err.println("DB init SQL error: " + e.getMessage() + " at line " + e.getStackTrace()[0].getLineNumber());
            e.printStackTrace();
            throw new RuntimeException("SQL connection failed", e);
        } catch (IOException e) {
            System.err.println("DB init IO error: " + e.getMessage() + " at line " + e.getStackTrace()[0].getLineNumber());
            e.printStackTrace();
            throw new RuntimeException("IO error during DB init", e);
        }
    }


    public static void insertFile(String name, long size, String hash) {
        if (conn == null) {
            throw new IllegalStateException("DB connection is not initialized");
        }
        String sql = "INSERT INTO files (name, size, hash) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setLong(2, size);
            pstmt.setString(3, hash);
            pstmt.executeUpdate();
            System.out.println("Inserted file: " + name + " (size: " + size + ", hash: " + hash + ")");
        } catch (SQLException e) {
            System.err.println("Insert error for " + name + ": " + e.getMessage() + " at line " + e.getStackTrace()[0].getLineNumber());
            e.printStackTrace();
            throw new RuntimeException("Insert failed", e);
        }
    }

    public static List<String[]> getAllFiles() {
        if (conn == null) {
            throw new IllegalStateException("DB connection is not initialized");
        }
        List<String[]> files = new ArrayList<>();
        String sql = "SELECT name, size, hash FROM files ORDER BY upload_date DESC";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                files.add(new String[]{rs.getString("name"), String.valueOf(rs.getLong("size")), rs.getString("hash")});
            }
        } catch (SQLException e) {
            System.err.println("Query all files error: " + e.getMessage() + " at line " + e.getStackTrace()[0].getLineNumber());
            e.printStackTrace();
        }
        return files;
    }

    public static String[] getFileInfo(String name) {
        if (conn == null) {
            throw new IllegalStateException("DB connection is not initialized");
        }
        String sql = "SELECT size, hash FROM files WHERE name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{String.valueOf(rs.getLong("size")), rs.getString("hash")};
                } else {
                    System.err.println("File not in DB: " + name);
                }
            }
        } catch (SQLException e) {
            System.err.println("Get file info error for " + name + ": " + e.getMessage() + " at line " + e.getStackTrace()[0].getLineNumber());
            e.printStackTrace();
        }
        return null;
    }

    public static void saveProgress(String fileName, int chunkIndex, long bytesDone) {
        if (conn == null) {
            throw new IllegalStateException("DB connection is not initialized");
        }
        String sql = "INSERT OR REPLACE INTO progress (file_name, chunk_index, bytes_done) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileName);
            pstmt.setInt(2, chunkIndex);
            pstmt.setLong(3, bytesDone);
            pstmt.executeUpdate();
            System.out.println("Saved progress for " + fileName + " chunk " + chunkIndex + ": " + bytesDone + " bytes");
        } catch (SQLException e) {
            System.err.println("Save progress error: " + e.getMessage() + " at line " + e.getStackTrace()[0].getLineNumber());
            e.printStackTrace();
        }
    }

    public static Map<Integer, Long> getProgress(String fileName) {
        if (conn == null) {
            throw new IllegalStateException("DB connection is not initialized");
        }
        Map<Integer, Long> progress = new HashMap<>();
        String sql = "SELECT chunk_index, bytes_done FROM progress WHERE file_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    progress.put(rs.getInt("chunk_index"), rs.getLong("bytes_done"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Get progress error for " + fileName + ": " + e.getMessage() + " at line " + e.getStackTrace()[0].getLineNumber());
            e.printStackTrace();
        }
        return progress;
    }

    public static void clearProgress(String fileName) {
        if (conn == null) {
            throw new IllegalStateException("DB connection is not initialized");
        }
        String sql = "DELETE FROM progress WHERE file_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileName);
            pstmt.executeUpdate();
            System.out.println("Cleared progress for " + fileName);
        } catch (SQLException e) {
            System.err.println("Clear progress error: " + e.getMessage() + " at line " + e.getStackTrace()[0].getLineNumber());
            e.printStackTrace();
        }
    }

    public static void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("DB closed");
                conn = null; // Reset connection để đảm bảo khởi tạo lại khi cần
            }
        } catch (SQLException e) {
            System.err.println("Close DB error: " + e.getMessage() + " at line " + e.getStackTrace()[0].getLineNumber());
            e.printStackTrace();
        }
    }
}