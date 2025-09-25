package com.example.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final String CONFIG_FILE = "application.properties";
    private static Properties properties;

    static {
        properties = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            } else {
                System.err.println("Warning: Could not find " + CONFIG_FILE + ", using default values");
                // Giá trị mặc định
                properties.setProperty("servers", "localhost:12347,localhost:12348");
                properties.setProperty("expected_hash", "3fad459e0dbaaea15a0845d18fbcc27fdb1ae83e64b6f2b4f78c12eae43f7a00");
                properties.setProperty("file_path", "test.txt");
            }
        } catch (IOException e) {
            System.err.println("Failed to load config file: " + e.getMessage());
            // Giá trị mặc định nếu có lỗi
            properties.setProperty("servers", "localhost:12347,localhost:12348");
            properties.setProperty("expected_hash", "3fad459e0dbaaea15a0845d18fbcc27fdb1ae83e64b6f2b4f78c12eae43f7a00");
            properties.setProperty("file_path", "test.txt");
        }
    }

    public static String[] getServers() {
        String serversStr = properties.getProperty("servers", "").trim();
        if (serversStr.isEmpty()) {
            System.err.println("Warning: No servers configured, using default");
            return new String[]{"localhost:12347", "localhost:12348"};
        }
        String[] servers = serversStr.split(",");
        for (int i = 0; i < servers.length; i++) {
            servers[i] = servers[i].trim();
        }
        return servers;
    }

    public static String getExpectedHash() {
        return properties.getProperty("expected_hash", "3fad459e0dbaaea15a0845d18fbcc27fdb1ae83e64b6f2b4f78c12eae43f7a00");
    }

    public static String getFilePath() {
        return properties.getProperty("file_path", "test.txt");
    }
}