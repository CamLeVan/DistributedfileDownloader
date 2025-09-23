package com.example.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static Properties props = new Properties();

    static {
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is == null) {
                System.err.println("Warning: application.properties not found, using defaults");
                props.setProperty("servers", "localhost:12345,localhost:12346");
                props.setProperty("expected_hash", "3fad459e0dbaaea15a0845d18fbcc27fdb1ae83e64b6f2b4f78c12eae43f7a00");
                props.setProperty("file_path", "test.txt");
            } else {
                props.load(is);
            }
        } catch (IOException e) {
            System.err.println("Failed to load config, using defaults: " + e.getMessage());
            props.setProperty("servers", "localhost:12345,localhost:12346");
            props.setProperty("expected_hash", "3fad459e0dbaaea15a0845d18fbcc27fdb1ae83e64b6f2b4f78c12eae43f7a00");
            props.setProperty("file_path", "test.txt");
        }
    }

    public static String[] getServers() {
        String serversStr = props.getProperty("servers", "").trim();
        if (serversStr.isEmpty()) {
            System.err.println("Warning: No servers configured, using default");
            return new String[]{"localhost:12345", "localhost:12346"};
        }
        return serversStr.split(",");
    }

    public static String getExpectedHash() {
        String hash = props.getProperty("expected_hash", "").trim();
        if (hash.isEmpty()) {
            System.err.println("Warning: No expected hash configured, verification will fail.");
        }
        return hash;
    }

    public static String getFilePath() {
        String path = props.getProperty("file_path", "test.txt").trim();
        if (path.isEmpty()) {
            System.err.println("Warning: File path not configured, using default 'test.txt'");
        }
        return path;
    }
}